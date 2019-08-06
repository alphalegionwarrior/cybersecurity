"""
OpenDrop: an open source AirDrop implementation
Copyright (C) 2018  Milan Stute
Copyright (C) 2018  Alexander Heinrich

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
"""
import io

import logging
import platform
import plistlib
import socket
from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import re
import libarchive
import libarchive.extract
import libarchive.read
from .zeroconf import Zeroconf, ServiceInfo
import time

from .util import AirDropUtil

logger = logging.getLogger(__name__)
devices = []

class AirDropServer:
    """
    Announces an HTTPS AirDrop server in the local network via mDNS.
    """

    def __init__(self, config):
        self.config = config

        if self.config.legacy:
            self.useIPv6 = False
        else:
            self.useIPv6 = True
        self.ip_interface_name = self.config.interface

        if self.useIPv6:
            self.address_family = socket.AF_INET6
            self.serveraddress = ('::', self.config.port)
            self.ServerClass = HTTPServerV6
        else:
            self.address_family = socket.AF_INET
            self.serveraddress = ('0.0.0.0', self.config.port)
            self.ServerClass = HTTPServer

        self.ServerClass.allow_reuse_address = False

        self.ip_addr, self.byte_address = AirDropUtil.get_ip_for_interface(self.ip_interface_name, ipv6=self.useIPv6)

        self.Handler = AirDropServerHandler
        self.Handler.config = self.config

        if self.config.legacy:
            self.zeroconf = Zeroconf()
        else:
            self.zeroconf = Zeroconf(interfaces=[self.ip_addr], ipv6_interface_name=self.ip_interface_name,
                                     apple_mdns=True)

        self.http_server = self._init_server()
        self.service_info = self._init_service()

    def _init_service(self):
        properties = self.get_properties()
        server = self.config.host_name + '.local.'
        service_name = self.config.service_id + '._airdrop._tcp.local.'
        info = ServiceInfo(
            '_airdrop._tcp.local.', service_name,
            self.byte_address, self.config.port, 0, 0, properties, server)
        return info

    def start_service(self):
        logger.info('Announcing service: host {}, address {}, port {}'.format(self.config.host_name,
                                                                              self.ip_addr, self.config.port))
        self.zeroconf.register_service(self.service_info)

    def _init_server(self):
        try:
            httpd = self.ServerClass(self.serveraddress, self.Handler)
        except OSError:
            # Address in use. Change port
            self.config.port = self.config.port + 1
            self.serveraddress = (self.serveraddress[0], self.config.port)
            httpd = self.ServerClass(self.serveraddress, self.Handler)

        # Adapt socket for awdl0
        if self.ip_interface_name == 'awdl0' and platform.system() == 'Darwin':
            httpd.socket.setsockopt(socket.SOL_SOCKET, 0x1104, 1)

        httpd.socket = self.config.get_ssl_context().wrap_socket(sock=httpd.socket, server_side=True)

        return httpd

    def start_server(self):
        logger.info('Starting HTTPS server')
        self.http_server.serve_forever()

    def stop(self):
        self.zeroconf.unregister_all_services()
        self.http_server.shutdown()

    def get_properties(self):
        properties = {b'flags': str(self.config.flags).encode('utf-8')}
        if self.config.legacy:
            properties[b'phash'] = AirDropUtil.doubleSHA1Hash(self.config.phone).encode('utf-8')
            properties[b'nhash'] = False
            properties[b'ehash'] = AirDropUtil.doubleSHA1Hash(self.config.email).encode('utf-8')
            properties[b'cname'] = self.config.computer_name.encode('utf-8')
        return properties


class HTTPServerV6(HTTPServer):
    address_family = socket.AF_INET6

def get_devices():
    return devices

class AirDropServerHandler(BaseHTTPRequestHandler):
    """
    Server which responds to AirDrop HTTP POST requests
    """
    protocol_version = 'HTTP/1.1'
    config = None

    def _set_response(self, content_length):
        """
        Setting the default values for a successful response
        """
        self.send_response(200)
        self.send_header('Content-Length', content_length)
        self.end_headers()

    def do_HEAD(self):
        """
        Answer head requests
        """
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def do_GET(self):
        """
        Answer get requests
        """
        logger.debug('GET request at {}'.format(self.path))
        body = '\n'.encode('utf-8')
        self._set_response(len(body))
        self.wfile.write(body)

    def print_info(self, post_data):
        print ("[*] Found one..")
        z = plistlib.loads(post_data, fmt=plistlib.FMT_BINARY)['SenderRecordData']
        result = re.search('<key>ValidatedPhoneHashes</key>(.*)</array>', str(z))
        rez = result.group(1)
        if rez:
            rez = rez.replace("<array>",'').replace("<string>",'').replace("</string>",'').replace("\\n",'').replace("\\t",'')
            # print (rez)
            devices.append({"ip":self.client_address[0],"hash":rez, "phone":""})
            logger.debug("IPv6:{}\nPhone hash: {}".format(self.client_address[0],rez))


    def handle_discover(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        AirDropUtil.write_debug(self.config, post_data, 'receive_discover_request.plist')
        self.print_info(post_data)
        # sample media capabilities as recorded from macOS 10.13.3
        media_capabilities = {
            'Version': 1,
            # don't advertise any codec/container support so we receive legacy file formats (JPEG instead of HEIF, etc.)
            # 'Codecs': {
            #     'hvc1': {
            #         'Profiles': {
            #             'VTPerProfileSupport': {
            #                 '1': {'VTMaxPlaybackLevel': 120},
            #                 '2': {'VTMaxPlaybackLevel': 120},
            #                 '3': {}
            #             },
            #             'VTSupportedProfiles': [1, 2, 3]
            #         }
            #     }
            # },
            # 'ContainerFormats': {
            #     'public.heif-standard': {
            #         'HeifSubtypes': ['public.avci', 'public.heic', 'public.heif']
            #     }
            # },
            # 'Vendor': {
            #     'com.apple': {
            #         'OSVersion': [10, 13, 3],
            #         'OSBuildVersion': '17D102',
            #         'LivePhotoFormatVersion': '1'
            #     }
            # }
        }
        media_capabilities_json = json.JSONEncoder().encode(media_capabilities)
        media_capabilities_binary = media_capabilities_json.encode('utf-8')
        discover_answer = {
            'ReceiverMediaCapabilities': media_capabilities_binary,
            'ReceiverComputerName': self.config.computer_name,
            'ReceiverModelName': self.config.computer_model,
        }
        if self.config.record_data:
            discover_answer['ReceiverRecordData'] = self.config.record_data

        discover_answer_binary = plistlib.dumps(discover_answer, fmt=plistlib.FMT_BINARY)

        AirDropUtil.write_debug(self.config, discover_answer_binary, 'receive_discover_response.plist')

        # Change to actual length
        self._set_response(len(discover_answer_binary))
        self.wfile.write(discover_answer_binary)

    def handle_ask(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)

        AirDropUtil.write_debug(self.config, post_data, 'receive_ask_request.plist')

        ask_response = {'ReceiverModelName': self.config.computer_model,
                        'ReceiverComputerName': self.config.computer_name}
        ask_resp_binary = plistlib.dumps(ask_response, fmt=plistlib.FMT_BINARY)

        AirDropUtil.write_debug(self.config, ask_resp_binary, 'receive_ask_response.plist')

        self._set_response(len(ask_resp_binary))
        self.wfile.write(ask_resp_binary)

    def handle_upload(self):
        if self.headers.get('content-type', '').lower() != 'application/x-cpio':
            logger.warning('Unsupported content-type: {}'.format(self.headers.get('content-type')))
            self.send_response(406)  # Unprocessable Entity
            self.send_header('Content-Type', 'application/x-cpio')
            self.send_header('Content-Length', 0)
            self.send_header('Connection', 'close')
            self.end_headers()
            return

        # If pipelining is not support, 'Expect: 100-continue' is sent to which we need to respond
        if self.headers.get('expect', '').lower() == '100-continue':
            self.send_response(100)
            self.send_header('Content-Length', 0)
            self.end_headers()

        if self.headers.get('transfer-encoding', '').lower() != 'chunked':
            logger.warning('Expect chunked transfer encoding')
            self.send_response(400)  # Bad Request
            self.send_header('Transfer-Encoding', 'Chunked')
            self.send_header('Content-Length', 0)
            self.send_header('Connection', 'close')
            self.end_headers()
            return

        class HTTPChunkedReader(io.RawIOBase):
            def __init__(self, rfile, *args, **kwargs):
                super().__init__(*args, **kwargs)
                self.rfile = rfile
                self.chunk = None
                self.total = 0

            def _next_chunk(self):
                if self.chunk is None or len(self.chunk) is 0:
                    length = int(self.rfile.readline().rstrip(), 16)
                    self.chunk = self.rfile.read(length)
                    self.rfile.readline()  # strip trailing \n\r

            def readinto(self, buf):
                self._next_chunk()
                l = min(len(self.chunk), len(buf))
                buf[:l] = self.chunk[:l]
                self.chunk = self.chunk[l:]
                self.total += l
                return l

        def extract_stream(stream, flags=0):
            """
            Extracts an archive from memory into the current directory.
            """

            with libarchive.read.stream_reader(stream) as archive:
                libarchive.extract.extract_entries(archive, flags)

        logger.info('Receiving file(s) ...')
        start = time.time()
        reader = HTTPChunkedReader(self.rfile)
        extract_stream(reader)

        transferred = reader.total / 1024.0 / 1024.0
        speed = transferred / (time.time() - start)
        logger.info('File(s) received (size {:.02f} MB, speed {:.02f} MB/s)'.format(transferred, speed))

        self.send_response(200)
        self.send_header('Content-Length', 0)
        self.send_header('Connection', 'close')
        self.end_headers()

    def do_POST(self):
        """
        Handle post requests
        """

        logger.debug('POST request at {}'.format(self.path))
        logger.debug('Headers\n{}'.format(self.headers))

        if self.path == '/Discover':
            self.handle_discover()
        elif self.path == '/Ask':
            self.handle_ask()
        elif self.path == '/Upload':
            self.handle_upload()
        else:
            answer = 'POST request for {}'.format(self.path).encode('utf-8')
            self._set_response(len(answer))
            self.wfile.write(answer)

    def log_message(self, format, *args):
        logger.debug('{} - - [{}] {}'.format(self.client_address[0], self.log_date_time_string(), format % args))

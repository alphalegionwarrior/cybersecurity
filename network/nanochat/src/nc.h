#ifndef NC_H
#define NC_H

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <getopt.h>
#include <string.h>
#include <time.h>
#include <sys/select.h>
#include <stdarg.h>
#include <pthread.h>
#include <pwd.h>

#include <sys/socket.h>
#include <arpa/inet.h>
#include <ifaddrs.h>

#include <readline/readline.h>
#include <readline/history.h>

#include <nanomsg/nn.h>
#include <nanomsg/reqrep.h>
#include <nanomsg/pair.h>

#include <vedis.h>
#include <sodium.h>
#include <parson.h>

#define DISCO_PORT "1881" /* not changable */
#define DEFAULT_RPC_PORT "1991"
#define INIT_OTOC_PORT 1992

#define HOST_MAX 20
#define URL_MAX 30
#define PORT_MAX 6
#define OTOC_PORT_LEN 6
#define NOW_STR_LEN 26
#define CONF_KEY_LEN 1024
#define CONF_VAL_LEN 1024

/* shell commands */
#define SCMD_NAME_LEN 32
#define SCMD_MAX 10

/* otoc shell commands */
#define OCMD_NAME_LEN 32
#define OCMD_MAX 10

/* discovery */
#define DCMD_BUF 64
#define DCMD_LEN 8
#define DCMD_CODE_LEN 2
#define DCMD_PROBE_ACK_CODE "00"
#define DCMD_PROBE_REQUEST_CODE "01"
#define DCMD_PROBE_RESPONSE_CODE "02"
#define DCMD_PROBE_TIMEOUT_SEC 3 

/* rpc commands */
#define RCMD_LEN 2
#define RCMD_OTOC "01"

/* one to one message types */
#define OTOC_MTYPE_LEN 8
#define OTOC_MTYPE_PKEY "publ_key" /* public key */
#define OTOC_MTYPE_RTXT "text_raw" /* raw text */
#define OTOC_MTYPE_STXT "text_sec" /* encrypted text */
#define OTOC_MTYPE_RFLE "file_raw" /* raw file */
#define OTOC_MTYPE_SFLE "file_sec" /* encrypted file */

/* names */
#define USERNAME_MAX 32
#define HOSTNAME_MAX 32

/* room */
#define ROOMS_LIMIT 16

/* global variablea */
extern unsigned char my_publickey[crypto_box_PUBLICKEYBYTES];
extern unsigned char my_secretkey[crypto_box_SECRETKEYBYTES];
extern unsigned char peers_publickey
                     [ROOMS_LIMIT]
                     [crypto_box_PUBLICKEYBYTES];

typedef struct nc_opts {
  char host[HOST_MAX];
  char broadcast[HOST_MAX];
  char port[PORT_MAX]; /* RPC port */
  char url[URL_MAX];
  int secure;
  int verbose;
  int discoverable;
} nc_opts;

typedef struct nc_conf_rec {
  char key[CONF_KEY_LEN];
  char val[CONF_VAL_LEN];
} nc_conf_rec;

typedef struct nc_shell_cmd {
  int code;
  char name[SCMD_NAME_LEN];
  int (*func)(char *cmd, nc_opts *opts);
} nc_shell_cmd;

typedef struct nc_otoc_cmd {
  int code;
  char name[OCMD_NAME_LEN];
  int (*func)(char *cmd);
} nc_otoc_cmd;

typedef struct nc_array {
  char **array_ptr;
  int array_len;
  int string_len;
} nc_array;

/* --- parameters --- */
void nc_param_get_opts(nc_opts *opts, int argc, char **argv);

/* --- logger --- */
void nc_log_start();
void nc_log_write(const char *tag, const char *msg);
void nc_log_writef(const char *tag, const char *msgf, ...);

/* --- utilities --- */
void nc_utils_print_help();
int nc_utils_count_char(const char *str, const char chr);
void nc_utils_now_str(char *time_str);
void nc_utils_del_new_line(char *str);
int nc_utils_has_new_line(char *str);
void nc_utils_make_url(char *url, char *host, char *port);
int nc_utils_get_rec_sockfd(int sock);
void nc_utils_empty_string(char *str);
void nc_utils_die(char *str);

/* --- disco --- */
void nc_disco_start(nc_opts *opts);
int nc_disco_probe(nc_opts *opts);

/* --- rpc --- */
void nc_rpc_start(nc_opts *opts);

/* --- shell --- */
void nc_shell_start(nc_opts *opts);
void nc_shell_register_cmd(char *name, int (*func)(char *cmd, nc_opts *opts));

/* --- one to one chat --- */
void nc_otoc_start(nc_opts *opts, int pair_raw_sock);
void nc_otoc_register_cmd(char *name, int (*func)(char *cmd));

/* --- config --- */
void nc_conf_start();
void nc_conf_get(nc_conf_rec *conf_rec);

/* --- network interfaces --- */
int nc_netif_get_addrs(char *inet, char *broadcast);

/* --- udp --- */
int nc_udp_send(char *ip, char *port, char *body, int broadcast);

/* --- names --- */
int nc_names_get_hostname(char *buf, int len);
int nc_names_get_username(char *buf, int len);

/* --- data access layer --- */
void nc_dal_start(nc_opts *opts);
int nc_dal_save_room(int room_code);
int nc_dal_save_peer(char **record);
int nc_dal_print_peers();
int nc_dal_print_rooms();

/* --- array --- */
nc_array* nc_array_string_new(int array_len, int string_len);
int nc_array_string_set(nc_array *array, int index, char *string);
char* nc_array_string_get(nc_array *array, int index);
int nc_array_string_len(nc_array *array);

/* --- crypto --- */
void nc_crypto_start(nc_opts *opts);

/* --- json --- */
int nc_json_extract_otoc_msg(char **msg, char **type, int *original_body_len, char **body);
int nc_json_make_otoc_msg(char **type, char **body, int original_body_len, char **msg);

/* --- message --- */
void nc_msg_encode(int head_len, int body_len, char *head, char *body, char **buf);
void nc_msg_free(char *buf);

#endif

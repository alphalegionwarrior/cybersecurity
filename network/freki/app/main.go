package main

import (
	"bufio"
	"net"
	"os"
	"os/signal"
	"sync"

	log "github.com/Sirupsen/logrus"
	docopt "github.com/docopt/docopt-go"
	"github.com/kung-foo/freki"
)

// VERSION is set by the makefile
var VERSION = "0.0.0"

var usage = `
Usage:
    freki [options] [-v ...] -i <interface> -r <rules>
    freki -h | --help | --version
Options:
    -i --interface=<iface>  Bind to this interface.
    -r --rules=<rules>      Rules file.
    -h --help               Show this screen.
    --version               Show version.
    -v                      Enable verbose logging (-vv for very verbose)
`

const (
	tcpLoggerPort      = 6000
	httpLoggerPort     = 6001
	tcpProxyPort       = 6002
	userConnServerPort = 6003
)

func main() {
	mainEx(os.Args[1:])
}

func onErrorExit(err error) {
	if err != nil {
		log.Fatalf("[freki   ] %+v", err)
	}
}

func onInterruptSignal(fn func()) {
	sig := make(chan os.Signal, 1)
	signal.Notify(sig, os.Interrupt)

	go func() {
		<-sig
		fn()
	}()
}

func mainEx(argv []string) {
	args, err := docopt.Parse(usage, argv, true, VERSION, true)
	onErrorExit(err)

	logger := log.New()

	if args["-v"].(int) > 0 {
		logger.Level = log.DebugLevel
	}

	if args["-v"].(int) > 1 {
		// TODO: trace level
	}

	freki.SetDefaultLogger(logger)

	rulesFile, err := os.Open(args["--rules"].(string))
	onErrorExit(err)

	rules, err := freki.ReadRulesFromFile(rulesFile)
	onErrorExit(err)

	processor, err := freki.New(args["--interface"].(string), rules, nil)
	onErrorExit(err)

	// TODO: this all needs to move _inside_ freki
	processor.AddServer(freki.NewTCPLogger(tcpLoggerPort, 1024))
	processor.AddServer(freki.NewHTTPLogger(httpLoggerPort))
	processor.AddServer(freki.NewTCPProxy(tcpProxyPort))
	processor.AddServer(freki.NewUserConnServer(userConnServerPort))

	processor.RegisterConnHandler("echo", func(conn net.Conn, md *freki.Metadata) error {
		defer conn.Close()
		host, _, _ := net.SplitHostPort(conn.RemoteAddr().String())
		logger.Infof("[main    ] echo request %s -> %d", host, uint(md.TargetPort))

		b := bufio.NewReader(conn)
		for {
			line, e := b.ReadBytes('\n')
			if e != nil {
				break
			}
			conn.Write(line)
		}
		return nil
	})

	err = processor.Init()
	onErrorExit(err)

	exitMtx := sync.RWMutex{}
	exit := func() {
		exitMtx.Lock()
		println() // make it look nice after the ^C
		logger.Debugf("[freki   ] shutting down...")
		onErrorExit(processor.Shutdown())
	}

	defer exit()
	onInterruptSignal(func() {
		exit()
		os.Exit(0)
	})

	err = processor.Start()
	if err != nil {
		log.Errorf("[freki   ] %+v", err)
	}
}

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Configuration;
using System.IO;
using Microsoft.ServiceBus.Messaging;
using System.Diagnostics;

namespace AzSbAmqp
{
    class Program
    {
        private string _sbNamespace = "";
        private string _sasKeyName = "";
        private string _sasKey = "";
        private string _connectionString = "";

        private bool _send = false;
        private bool _recv = false;
        private string _queueName = "";
        private int _loopCount = 1;
        private bool _infinite = false;
        private string _msgBase = "Test Message";

        private void TraceLine(string format, params object[] args)
        {
            DateTime dt = DateTime.Now;
            string msg = string.Format(format, args);
            Console.WriteLine("{0} [AzSbAmqp]: {1}", dt.ToLongTimeString(), msg);
        }
        private int FindArg(string[] args, string argument)
        {
            if (argument.StartsWith("/") || argument.StartsWith("-"))
                argument = argument.Substring(1);
            for (int n = 0; n < args.Length; n++)
            {
                string arg = args[n];
                if (arg.StartsWith("/") || arg.StartsWith("-"))
                    arg = arg.Substring(1);
                if (string.Compare(arg, argument, true) == 0)
                    return n;
            }
            return -1;
        }
        private bool GetArgsAndConfig(string[] args)
        {
            bool rc = true;
            int idx;

            _sasKeyName = ConfigurationManager.AppSettings["SharedAccessKeyName"];
            _sasKey = ConfigurationManager.AppSettings["SharedAccessKey"];
            _sbNamespace = ConfigurationManager.AppSettings["ServiceNamespace"];

            if (-1 != (idx = FindArg(args, "SharedAccessKeyName")))
            {
                _sasKeyName = args[idx + 1];
            }
            if (-1 != (idx = FindArg(args, "SharedAccessKey")))
            {
                _sasKey = args[idx + 1];
            }
            if (-1 != (idx = FindArg(args, "ServiceNamespace")))
            {
                _sbNamespace = args[idx + 1];
            }
            if (-1 != (idx = FindArg(args, "send")))
            {
                _send = true;
            }
            if (-1 != (idx = FindArg(args, "recv")))
            {
                _recv = true;
            }
            if (-1 != (idx = FindArg(args, "queue")))
            {
                _queueName = args[idx + 1];
            }
            if (-1 != (idx = FindArg(args, "loop")))
            {
                _loopCount = int.Parse(args[idx + 1]);
            }
            if (-1 != (idx = FindArg(args, "infinite")))
            {
                _infinite = true;
            }
            if (-1 != (idx = FindArg(args, "msg")))
            {
                _msgBase = args[idx + 1];
            }

            _connectionString = ConfigurationManager.AppSettings["sbConnectionString"].ToString();
            _connectionString = _connectionString.Replace("%namespace%", _sbNamespace );
            _connectionString = _connectionString.Replace("%sasname%", _sasKeyName );
            _connectionString = _connectionString.Replace("%saskey%", _sasKey);

            return rc;
        }

        static void Main(string[] args)
        {
            Program p = new AzSbAmqp.Program();
            p.Go(args);
        }

        private void Go(string[] args)
        {
            GetArgsAndConfig(args);
            if (!_send && !_recv)
            {
                Console.WriteLine("Must specify -send or -recv on command line");
                return;
            }
            Console.ForegroundColor = ConsoleColor.Blue;
            TraceLine("Opening queue {0} in namespace {1}", _queueName, _sbNamespace);
            Stopwatch sw = new Stopwatch();
            sw.Start();
            QueueClient queue = QueueClient.CreateFromConnectionString(_connectionString, _queueName);
            sw.Stop();
            TraceLine("Time {0} ms", sw.ElapsedMilliseconds);
            Console.ResetColor();

            if ( _send )
            {
                Sender( queue );
            }
            if ( _recv )
            {
                Receiver( queue );
            }
        }
        private void Sender(QueueClient queue)
        {

            string msgId = "";
            string message = "";
            Stopwatch sw = new Stopwatch();

            for (int n = 1; n <= _loopCount; n++)
            {
                Console.ForegroundColor = ConsoleColor.Yellow;
                TraceLine("Sending Message #{0}...", n + 1);
                sw = new Stopwatch();
                sw.Start();
                message = string.Format( "[{0}] - {1}", n, _msgBase );
                bool rc = SendMessage( queue, message, out msgId);
                sw.Stop();
                TraceLine("SendRequest(). Time {0} ms. MsgId {1}", sw.ElapsedMilliseconds, msgId);
                Console.ResetColor();
                if (!rc)
                    break;
            }
        }
        public bool SendMessage(QueueClient queue, string message, out string msgId)
        {
            bool rc = false;
            msgId = "";
            BrokeredMessage bm = new BrokeredMessage( message );
            msgId = bm.MessageId;
            try
            {
                queue.Send(bm);
                rc = true;
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
            return rc;
        }
        private void Receiver(QueueClient queue)
        {
            queue.OnMessage(message =>
            {
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine(String.Format("Message id: {0}", message.MessageId));
                Console.WriteLine(String.Format("Message body: {0}", message.GetBody<String>()));
                Console.ResetColor();
            });

            Console.ReadLine();
        }

    } // cls
} // ns

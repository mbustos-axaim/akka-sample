using System;
using System.Reflection;
using System.IO;
using System.Collections.Generic;
using log4net;
using log4net.Config;

namespace WinTail
{
    #region Program
    class Program
    {
        static void Main(string[] args)
        {
            var logRepository = LogManager.GetRepository(Assembly.GetEntryAssembly());
            XmlConfigurator.Configure(logRepository, new FileInfo("log4net.config"));

            List<DagSpec> dag = new List<DagSpec>();
            dag.Add(new DagSpec("a", new List<string>(), "payload a", 2000));
            dag.Add(new DagSpec("b", new List<string>(new string[] { "a" }), "payload b", 1000));
            dag.Add(new DagSpec("c", new List<string>(new string[] { "a", "b" }), "payload c", 5000));
            dag.Add(new DagSpec("d", new List<string>(new string[] { "c", "a" }), "payload d", 5000));
            dag.Add(new DagSpec("e", new List<string>(new string[] { "d", "f" }), "payload e", 10000));
            dag.Add(new DagSpec("f", new List<string>(), "payload f", 23000));

            DagManager manager = new DagManager(dag);
        }

    }
    #endregion
}

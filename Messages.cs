using System;
using Akka.Actor;
using System.Collections.Generic;

namespace WinTail
{

    public class DagSpec
    {
        public DagSpec(string id, List<string> precursors, string payload, long delay = 0L, bool fail = false)
        {
            _id = id;
            _precursors = precursors;
            _payload = payload;
            _delay = delay;
            _fail = fail;
        }

        public string _id { get; set; }
        public List<string> _precursors { get; private set; }
        public string _payload { get; private set; }
        public long _delay { get; private set; }
        public bool _fail { get; private set; }
    }

    public class ConfigDeps
    {
        public ConfigDeps(List<IActorRef> deps)
        {
            _deps = deps;
        }

        public List<IActorRef> _deps { get; private set; }
    }

    public class Kick 
    {
        public Kick(string from)
        {
            _from = from;
        }

        public string _from { get; private set; }
    }

    public class ReportStart
    {
        public ReportStart(string id, IActorRef dag)
        {
            _id = id;
            _dag = dag;
        }

        public string _id { get; private set; }
        public IActorRef _dag { get; private set; }
    }

    public class ReportDone
    {
        public ReportDone(string id, IActorRef dag)
        {
            _id = id;
            _dag = dag;
        }

        public string _id { get; private set; }
        public IActorRef _dag { get; private set; }
    }

    public class ReportError
    {
        public ReportError(string id, IActorRef dag, Exception cause)
        {
            _id = id;
            _dag = dag;
            _cause = cause;
        }

        public string _id { get; private set; }
        public Exception _cause { get; private set; }
        public IActorRef _dag { get; private set; }
    }

    public class Cancel { };
    public class DagCompletesQuery { };
    public class DagFailuresQuery { };

}

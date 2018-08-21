using System;
using Akka.Actor;
using System.Collections.Generic;

namespace WinTail
{
    public class DagStatus : UntypedActor
    {
        public DagStatus()
        {
        }

        public List<string> completes = new List<string>();
        public List<string> failures = new List<string>();
        public List<IActorRef> working = new List<IActorRef>();

        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        protected override void OnReceive(object message)
        {
            if (message is ReportDone)
            {
                completes.Add(((ReportDone)message)._id);
                working.Remove(((ReportDone)message)._dag);
                log.Info("Completed: " + ((ReportDone)message)._id);
            }
            if (message is ReportError)
            {
                failures.Add(((ReportError)message)._id);
                working.Remove(((ReportError)message)._dag);
            }
            if (message is ReportStart)
            {
                working.Add(((ReportStart)message)._dag);
            }
            if (message is DagCompletesQuery)
            {
                Sender.Tell(completes);
            }
            if (message is DagFailuresQuery)
            {
                Sender.Tell(failures);
            }
        }

    }
}

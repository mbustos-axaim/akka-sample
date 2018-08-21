using System;
using Akka.Actor;
using System.Collections.Generic;
using System.Linq;

namespace WinTail
{
    public class DagNode : UntypedActor
    {
        public DagNode(string id, List<string> precursors, string payload, IActorRef monitor, long delay, bool fail)
        {
            _id = id;
            _precursors = precursors;
            _payload = payload;
            _monitor = monitor;
            _delay = delay;
            _fail = fail;
        }

        string _id { get; set; }
        List<string> _precursors { get; set; }
        string _payload { get; set; }
        IActorRef _monitor { get; set; }
        long _delay { get; set; }
        bool _fail { get; set; }

        List<IActorRef> _post = new List<IActorRef>();
        List<string> _kicks = new List<string>();

        protected override void OnReceive(object message)
        {
            if (message is ConfigDeps) 
            {
                _post = ((ConfigDeps)message)._deps;
                Become(active);
            }
        }

        private void active(object message) 
        {
            if (message is Kick)
            {
                _kicks.Add(((Kick)message)._from);
                if (_precursors.Count == 0)
                {
                    scheduleTask();
                } 
                else 
                {
                    List<string> toSchedule = _precursors.Except(_kicks).ToList();
                    if (toSchedule.Count == 0) {
                        scheduleTask();
                    }
                    else if (toSchedule.Count == 1) {
                        _monitor.Tell(new ReportStart(_id, Self));     
                    }
                }
            } 
            else if (message is Status.Success)
            {
                _monitor.Tell(new ReportDone(_id, Self));
                _post.ForEach(delegate (IActorRef post)
                {
                    post.Tell(new Kick(_id), Self);
                });
                Self.Tell(PoisonPill.Instance);
            }
            else if (message is Status.Failure)
            {
                _monitor.Tell(new ReportError(_id, Self, ((Status.Failure)message).Cause));
                Self.Tell(PoisonPill.Instance);
            }
        }

        private void scheduleTask()
        {
            if (_fail) Context.System.Scheduler.ScheduleTellOnce(TimeSpan.FromMilliseconds(_delay), Self, new Status.Failure(new Exception("Failure")), ActorRefs.Nobody);
            else Context.System.Scheduler.ScheduleTellOnce(TimeSpan.FromMilliseconds(_delay), Self, new Status.Success(_payload), ActorRefs.Nobody);
        }

        public static Props Props(string id, List<string> precursors, string payload, IActorRef monitor, long delay, bool fail)
        {
            return Akka.Actor.Props.Create(() => new DagNode(id, precursors, payload, monitor, delay, fail));
        }
    }

}


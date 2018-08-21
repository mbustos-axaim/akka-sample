using System;
using System.Collections.Generic;
using System.Linq;
using Akka.Actor;

namespace WinTail
{
    public class DagManager
    {
        public DagManager(List<DagSpec> input)
        {
            _system = ActorSystem.Create("dag");
            _monitor = _system.ActorOf<DagStatus>();

            dagDeps = input.Select(node => Tuple.Create(node._precursors, node._id)).ToList();
            dagRealized = 
                input.Select(node => Tuple.Create(node._id, 
                                                  _system.ActorOf(DagNode.Props(node._id, 
                                                                                node._precursors, 
                                                                                node._payload, 
                                                                                _monitor, 
                                                                                node._delay, 
                                                                                node._fail), node._id + "_executor"))).ToDictionary(x => x.Item1, x => x.Item2);

            startNodes = input.Where(x => x._precursors.Count == 0).Select(x => x._id).ToList();

            foreach (var item in dagRealized)
            {
                List<string> toKick = dagDeps.Where(x => x.Item1.Contains(item.Key)).Select(x => x.Item2).ToList();
                if (toKick.Count > 0)
                {
                    var that = dagRealized.Where(n => toKick.Contains(n.Key)).Select(x => x.Value).ToList();
                    toKick.ForEach(x => item.Value.Tell(new ConfigDeps(that)));
                }
                else
                {
                    item.Value.Tell(new ConfigDeps(new List<IActorRef>()));
                }
            }
/*
  val terminal_nodes: immutable.Iterable[ActorRef] = dagRealized.flatMap {
    case (id, node) => {
             dagDeps.filter(_._1.contains(id)).map(_._2) match {
        case depsIds: List[String] if depsIds.nonEmpty => {
                     // this is where the node has dependencies
                     node !ConfigDeps(dagRealized.filter(n => depsIds.contains(n._1)).values.toList) // find the dependencies of each node
          None
        }
        case _ => {
                 // these are the DAG end nodes
                 node !ConfigDeps(List[ActorRef]())
          Some(node)
        }
             }
         }
            }
*/

            foreach (KeyValuePair<string, IActorRef> entry in dagRealized)
            {
                if (startNodes.Contains(entry.Key)) 
                {
                    entry.Value.Tell(new Kick("$starter"));
                }
            }
            //_monitor.Tell(PoisonPill.Instance);
            //_system.Terminate();      
            _system.AwaitTermination();
        }

        private ActorSystem _system;
        private IActorRef _monitor;
        private List<Tuple<List<string>, string>> dagDeps;
        private Dictionary<string, IActorRef> dagRealized;
        private List<string> startNodes;
    }
}


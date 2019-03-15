package org.scribble.lang.local;

import java.util.HashSet;
import java.util.Set;

import org.scribble.ast.ProtocolKindNode;
import org.scribble.job.ScribbleException;
import org.scribble.lang.Continue;
import org.scribble.lang.STypeInliner;
import org.scribble.lang.STypeUnfolder;
import org.scribble.lang.Substitutions;
import org.scribble.model.endpoint.EGraphBuilderUtil2;
import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.type.kind.Local;
import org.scribble.type.name.RecVar;
import org.scribble.type.name.Role;

public class LContinue extends Continue<Local> implements LType
{
	public LContinue(//org.scribble.ast.Continue<Local> source, 
			ProtocolKindNode<Local> source,  // Due to inlining, do -> continue
			RecVar recvar)
	{
		super(source, recvar);
	}
	
	@Override
	public LContinue reconstruct(org.scribble.ast.ProtocolKindNode<Local> source,
			RecVar recvar)
	{
		return new LContinue(source, recvar);
	}

	@Override
	public LContinue substitute(Substitutions<Role> subs)
	{
		return (LContinue) super.substitute(subs);
	}

	@Override
	public LContinue getInlined(STypeInliner i)//, Deque<SubprotoSig> stack)
	{
		return (LContinue) super.getInlined(i);
	} 

	@Override
	public LRecursion unfoldAllOnce(STypeUnfolder<Local> u)
	{
		return new LRecursion(getSource(), this.recvar,  // CHECKME: Continue (not Recursion) as the source of the unfolding
				(LSeq) u.getRec(this.recvar));  
			
	}
	
	@Override
	public void buildGraph(EGraphBuilderUtil2 b)
	{
		// CHECKME: identical edges, i.e. same pred/prev/succ (e.g. rec X { choice at A { A->B:1 } or { A->B:1 } continue X; })  
		// Choice-guarded continue -- choice-unguarded continue detected and handled in LChoice
		EState curr = b.getEntry();
		for (EState pred : b.getPredecessors(curr))  // Does getAllSuccessors
		{
			for (EAction a : pred.getAllActions())
			{
				try
				{
					b.removeEdge(pred, a, curr);  //b.removeEdgeFromPredecessor(pred, prev);
					//b.addEdge(pred, a, entry);   //b.addRecursionEdge(pred, prev, b.getRecursionEntry(this.recvar));
					b.addRecursionEdge(pred, a, this.recvar);
				}
				catch (ScribbleException e)  // CHECKME
				{
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public ReachabilityEnv checkReachability(ReachabilityEnv env)
			throws ScribbleException
	{
		Set<RecVar> tmp = new HashSet<>(env.recvars);
		tmp.add(this.recvar);
		return new ReachabilityEnv(true, tmp);
	}
 
	@Override
	public int hashCode()
	{
		int hash = 3457;
		hash = 31 * hash + super.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof LContinue))
		{
			return false;
		}
		return super.equals(o);
	}

	@Override
	public boolean canEquals(Object o)
	{
		return o instanceof LContinue;
	}
}

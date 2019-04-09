/**
 * Copyright 2008 The Scribble Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.scribble.core.type.session.global;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.runtime.tree.CommonTree;
import org.scribble.core.type.kind.Global;
import org.scribble.core.type.name.Role;
import org.scribble.core.type.session.Choice;
import org.scribble.core.type.session.local.LChoice;
import org.scribble.core.type.session.local.LSeq;
import org.scribble.core.type.session.local.LSkip;
import org.scribble.core.type.session.local.LType;
import org.scribble.core.visit.global.Projector;

public class GChoice extends Choice<Global, GSeq> implements GType
{
	public GChoice(CommonTree source, Role subj,
			List<GSeq> blocks)
	{
		super(source, subj, blocks);
	}
	
	@Override
	public GChoice reconstruct(CommonTree source, Role subj,
			List<GSeq> blocks)
			//List<? extends Seq<Global>> blocks)
	{
		return new GChoice(source, subj, blocks);
	}

	/*@Override
	public <T> GChoice visit(Function<SType<Global, GSeq>, Stream<T>> f)
	{
		Stream<T> tmp = f.apply(this);
		if (tmp.count() != 1)
		{
			throw new RuntimeException(": " + tmp.collect(Collectors.toList()));
		}
		Role subj = (Role) tmp.findFirst().get();
		List<GSeq> blocks = this.blocks.stream()
				.map(x -> (GSeq) x.visit(f)).collect(Collectors.toList());
		return reconstruct(getSource(), subj, blocks);
	}*/
	
	@Override
	public LType projectInlined(Role self)
	{
		return projectAux(self,
				this.blocks.stream().map(x -> x.projectInlined(self)));
	}
	
	private LType projectAux(Role self, Stream<LSeq> blocks)
	{
		Role subj = this.subj.equals(self) ? Role.SELF : this.subj;  
				// CHECKME: "self" also explcitily used for Do, but implicitly for MessageTransfer, inconsistent?
		List<LSeq> tmp = blocks
				.filter(x -> !x.isEmpty())
				.collect(Collectors.toList());
		if (tmp.isEmpty())
		{
			return LSkip.SKIP;  // CHECKME: OK, or "empty" choice at subj still important?
		}
		return new LChoice(null, subj, tmp);
	}
	
	@Override
	public LType project(Projector v)
	{
		return projectAux(v.self, this.blocks.stream().map(x -> x.project(v)));
	}

	@Override
	public int hashCode()
	{
		int hash = 3067;
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
		if (!(o instanceof GChoice))
		{
			return false;
		}
		return super.equals(o);
	}

	@Override
	public boolean canEquals(Object o)
	{
		return o instanceof GChoice;
	}
}
















/*
	@Override
	public Set<Role> checkRoleEnabling(Set<Role> enabled) throws ScribException
	{
		if (!enabled.contains(this.subj))
		{
			throw new ScribException("Subject not enabled: " + this.subj);
		}
		Set<Role> subj = Stream.of(this.subj).collect(Collectors.toSet());
		List<Set<Role>> blocks = new LinkedList<>();
		for (GSeq block : this.blocks)
		{
			blocks.add(block.checkRoleEnabling(subj));
		}
		Set<Role> res = new HashSet<>(enabled);
		Set<Role> tmp = blocks.stream().flatMap(x -> x.stream())
				.filter(x -> blocks.stream().allMatch(y -> y.contains(x)))
				.collect(Collectors.toSet());
		res.addAll(tmp);
		return Collections.unmodifiableSet(res);
	}

	@Override
	public Map<Role, Role> checkExtChoiceConsistency(Map<Role, Role> enablers)
			throws ScribException
	{
		Map<Role, Role> subj = Stream.of(this.subj)
				.collect(Collectors.toMap(x -> x, x -> x));
		List<Map<Role, Role>> blocks = new LinkedList<>();
		for (GSeq block : this.blocks)
		{
			blocks.add(block.checkExtChoiceConsistency(subj));
		}
		Map<Role, Role> res = new HashMap<>(enablers);
		Set<Entry<Role, Role>> all = blocks.stream()
				.flatMap(x -> x.entrySet().stream()).collect(Collectors.toSet());
		for (Entry<Role, Role> e : all)
		{
			Role enabled = e.getKey();
			Role enabler = e.getValue();
			if (all.stream().anyMatch(
					x -> x.getKey().equals(enabled) && !x.getValue().equals(enabler)))
			{
				throw new ScribException(
						"Inconsistent external choice subjects for " + enabled + ": "
								+ all.stream().filter(x -> x.getKey().equals(enabled))
										.collect(Collectors.toList()));
			}
			if (!res.containsKey(enabled))
			{
				res.put(enabled, enabler);
			}
		}
		return Collections.unmodifiableMap(res);
	}
	}*/
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
package org.scribble.core.type.session;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.runtime.tree.CommonTree;
import org.scribble.core.type.kind.NonRoleParamKind;
import org.scribble.core.type.kind.ProtocolKind;
import org.scribble.core.type.name.DataType;
import org.scribble.core.type.name.MemberName;
import org.scribble.core.type.name.MessageSigName;
import org.scribble.core.type.name.ProtocolName;
import org.scribble.core.type.name.Role;
import org.scribble.core.type.name.Substitutions;
import org.scribble.core.visit.STypeVisitor;

public abstract class Do
		<K extends ProtocolKind, B extends Seq<K, B>, N extends ProtocolName<K>>
		extends STypeBase<K, B>
{
	public final N proto;  // Currently disamb'd to fullname by GTypeTranslator (see GDoDel::translate)
	public final List<Role> roles;  // Ordered role args; pre: size > 2
	public final List<Arg<? extends NonRoleParamKind>> args;
			// NonRoleParamKind, not NonRoleArgKind, because latter includes AmbigKind due to parsing requirements

	public Do(CommonTree source, N proto,
			List<Role> roles, List<Arg<? extends NonRoleParamKind>> args)
	{
		super(source);
		this.proto = proto;
		this.roles = Collections.unmodifiableList(roles);
		this.args = Collections.unmodifiableList(args);
	}

	public abstract Do<K, B, N> reconstruct(CommonTree source,
			N proto, List<Role> roles, List<Arg<? extends NonRoleParamKind>> args);
	
	@Override
	public <T> Stream<T> gather(Function<SType<K, B>, Stream<T>> f)
	{
		return f.apply(this);
	}

	@Override
	public SType<K, B> visitWith(STypeVisitor<K, B> v)
	{
		return v.visitDo(this);
	}

	@Override
	public Do<K, B, N> substitute(Substitutions subs)
	{
		List<Role> roles = this.roles.stream().map(x -> subs.subsRole(x))
				.collect(Collectors.toList());
		List<Arg<? extends NonRoleParamKind>> args = new LinkedList<>();
		for (Arg<? extends NonRoleParamKind> a : this.args) 
		{
			if (a instanceof MemberName<?> && subs.hasArg((MemberName<?>) a))
			{
				if (a instanceof DataType)
				{
					a = subs.subsArg((DataType) a);
				}
				else if (a instanceof MessageSigName)
				{
					a = subs.subsArg((MessageSigName) a);
				}
			}
			args.add(a);
		}
		return reconstruct(getSource(), this.proto, roles, args);
	}

	@Override
	public Do<K, B, N> pruneRecs()
	{
		return this;
	}
	
	@Override
	public String toString()
	{
		return "do " + this.proto 
				+ "<"
				+ this.args.stream().map(x -> x.toString())
						.collect(Collectors.joining(", "))
				+ ">"
				+ "(" + this.roles.stream().map(x -> x.toString())
						.collect(Collectors.joining(", "))
				+ ");";
	}

	@Override
	public int hashCode()
	{
		int hash = 193;
		hash = 31 * hash + super.hashCode();
		hash = 31 * hash + this.proto.hashCode();
		hash = 31 * hash + this.roles.hashCode();
		hash = 31 * hash + this.args.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Do))
		{
			return false;
		}
		Do<?, ?, ?> them = (Do<?, ?, ?>) o;
		return super.equals(this)  // Does canEquals
				&& this.proto.equals(them.proto) && this.roles.equals(them.roles) 
				&& this.args.equals(them.args);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	

	/*@Override
	public Set<Role> getRoles()
	{
		return this.roles.stream().collect(Collectors.toSet());
	}

	@Override
	public Set<MessageId<?>> getMessageIds()
	{
		Set<MessageId<?>> mids = new HashSet<>();
		for (Arg<? extends NonRoleParamKind> a : this.args)
		{
			if (a instanceof Message)
			{
				mids.add(((Message) a).getId());
			}
		}
		return mids;
	}

	@Override
	public Set<RecVar> getRecVars()
	{
		return Collections.emptySet();
	}
	
	@Override
	public SType<K, B> unfoldAllOnce(STypeUnfolder<K> u)
	{
		throw new RuntimeException("Unsupported for Do: " + this);
	}

	@Override
	public List<ProtocolName<K>> getProtoDependencies()
	{
		return Stream.of(this.proto).collect(Collectors.toList());
	}

	@Override
	public List<MemberName<?>> getNonProtoDependencies()
	{
		return this.args.stream()
				.filter(x -> (x instanceof MessageSig) || (x instanceof DataType))  // CHECKME: refactor?
				.map(x -> (MemberName<?>) x).collect(Collectors.toList());
	}
	*/
}

package org.scribble.lang.local;

import org.scribble.lang.SType;
import org.scribble.lang.STypeInliner;
import org.scribble.lang.STypeUnfolder;
import org.scribble.lang.Substitutions;
import org.scribble.type.kind.Local;
import org.scribble.type.name.Role;

public interface LType extends SType<Local>
{

	@Override
	LType substitute(Substitutions<Role> subs);

	@Override
	LType getInlined(STypeInliner i);//, Deque<SubprotoSig> stack);

	@Override
	default SType<Local> unfoldAllOnce(
			STypeUnfolder<Local> u)
	{
		throw new RuntimeException("Not supported for: " + this);
	}
}

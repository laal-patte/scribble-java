package org.scribble.ext.go.core.codegen.statetype3;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble.ast.DataTypeDecl;
import org.scribble.codegen.statetype.STActionBuilder;
import org.scribble.codegen.statetype.STStateChanApiBuilder;
import org.scribble.ext.go.core.ast.RPCoreDelegDecl;
import org.scribble.ext.go.core.model.endpoint.action.RPCoreECrossReceive;
import org.scribble.ext.go.core.model.endpoint.action.RPCoreECrossSend;
import org.scribble.ext.go.core.type.RPIndexedRole;
import org.scribble.ext.go.core.type.RPInterval;
import org.scribble.ext.go.core.type.RPRoleVariant;
import org.scribble.ext.go.type.index.RPIndexExpr;
import org.scribble.ext.go.type.index.RPIndexInt;
import org.scribble.ext.go.type.index.RPIndexVar;
import org.scribble.model.MState;
import org.scribble.model.endpoint.EGraph;
import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.EStateKind;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.type.name.DataType;
import org.scribble.type.name.GProtocolName;
import org.scribble.type.name.PayloadElemType;

// Duplicated from org.scribble.ext.go.codegen.statetype.go.GoSTStateChanAPIBuilder
public class RPCoreSTStateChanApiBuilder extends STStateChanApiBuilder
{
	protected final RPCoreSTApiGenerator apigen;
	public final RPRoleVariant variant;
	
	//public final RPCoreSTReceiveActionBuilder vb;
	
	private int counter = 1;
	private final Set<DataTypeDecl> dtds; // FIXME: use "main.getDataTypeDecl((DataType) pt);" instead -- cf. OutputSocketGenerator#addSendOpParams
	
	// N.B. the base EGraph class will probably be replaced by a more specific (and more helpful) rp-core class later
	// Pre: variant.getName().equals(this.role)
	public RPCoreSTStateChanApiBuilder(RPCoreSTApiGenerator apigen, RPRoleVariant variant, EGraph graph)
	{
		super(apigen.job, apigen.proto, apigen.self, graph,
				new RPCoreSTOutputStateBuilder(new RPCoreSTSplitActionBuilder(), new RPCoreSTSendActionBuilder()),
				new RPCoreSTReceiveStateBuilder(new RPCoreSTReduceActionBuilder(), new RPCoreSTReceiveActionBuilder()),
				new RPCoreSTSelectStateBuilder(new RPCoreSTSelectActionBuilder()),
				null, //new GoSTCaseBuilder(new GoSTCaseActionBuilder()),
				new RPCoreSTEndStateBuilder());

		this.apigen = apigen;
		this.variant = variant;
		
		//this.vb = ((RPCoreSTReceiveStateBuilder) this.rb).vb;
		
		this.dtds = this.apigen.job.getContext().getMainModule().getNonProtocolDecls().stream()
				.filter(d -> (d instanceof DataTypeDecl)).map(d -> ((DataTypeDecl) d)).collect(Collectors.toSet());
	}
	
	@Override
	public Map<String, String> build()  // filepath -> source
	{
		Map<String, String> res = new HashMap<>();
		Set<EState> states = new LinkedHashSet<>();
		states.add(this.graph.init);
		states.addAll(MState.getReachableStates(this.graph.init));
		for (EState s : states)
		{
			switch (RPCoreSTStateChanApiBuilder.getStateKind(s))
			{
				case CROSS_SEND: res.put(getFilePath(getStateChanName(s)), this.ob.build(this, s)); break;
				case CROSS_RECEIVE: 
				{
					if (s.getActions().size() > 1)
					{
						res.put(getFilePath(getStateChanName(s)), this.bb.build(this, s));
					}
					else
					{
						res.put(getFilePath(getStateChanName(s)), this.rb.build(this, s));
					}
					break;
				}
				/*case DOT_SEND:  // FIXME: CFSMs should have only !^1, ! and ?
				{
					throw new RuntimeException("[rp-core] TODO: " + s);
				}
				case DOT_RECEIVE:
				{
					throw new RuntimeException("[rp-core] TODO: " + s);
				}
				case MULTICHOICES_RECEIVE:
				{
					throw new RuntimeException("[rp-core] TODO: " + s);
				}*/
				case TERMINAL: res.put(getFilePath(getStateChanName(s)), this.eb.build(this, s)); break;
				default: throw new RuntimeException("[rp-core] Shouldn't get in here: " + s);
			}
		}
		return res;
	}

	@Override
	public String getFilePath(String filename)
	{
		if (filename.startsWith("_"))  // Cannot use "_" prefix, ignored by Go
		{
			filename = "$" + filename.substring(1);
		}
		return this.gpn.toString().replaceAll("\\.", "/") 
				+ "/" + RPCoreSTApiGenerator.getEndpointKindPackageName(this.variant)  // State chans located with Endpoint Kind API
				+ "/" + filename + ".go";
	}
	
	protected String getStateChanPremable(EState s)
	{
		GProtocolName simpname = this.apigen.proto.getSimpleName();
		String scTypeName = this.getStateChanName(s);
		String epkindTypeName = RPCoreSTApiGenerator.getEndpointKindTypeName(simpname, this.variant); 
		
		String res =
				  "package " + RPCoreSTApiGenerator.getEndpointKindPackageName(this.variant) + "\n"
				+ "\n"
				+ "import \"" + RPCoreSTApiGenConstants.GO_SCRIBBLERUNTIME_SESSION_PACKAGE + "\"\n"

				// FIXME: error handling via Err field -- fallback should be panic
				+ "import \"log\"\n"
				
				// FIXME: refactor into state-specific builders
				+ ((s.getStateKind() == EStateKind.OUTPUT || s.getStateKind() == EStateKind.UNARY_INPUT || s.getStateKind() == EStateKind.POLY_INPUT)
						? s.getActions().stream().flatMap(a -> a.payload.elems.stream()).collect(Collectors.toSet()).stream()
								.map(p -> makeExtNameImport((DataType) p)).collect(Collectors.joining(""))
						: "")

				// FIXME: refactor into state-specific builders
				+ ((s.getStateKind() == EStateKind.UNARY_INPUT || s.getStateKind() == EStateKind.POLY_INPUT)
						? "import \"sort\"\n\nvar _ = sort.Sort\n"
						: "")

				// State channel type
				+ "\n"
				+ "type " + scTypeName + " struct {\n"
				+ RPCoreSTApiGenConstants.GO_SCHAN_LINEARRESOURCE + " *" + RPCoreSTApiGenConstants.GO_LINEARRESOURCE_TYPE +"\n"
				+ RPCoreSTApiGenConstants.GO_SCHAN_ENDPOINT + " *" + epkindTypeName + "\n" 
				+ "}\n";

		return res;
	}

  // Here because action builder hierarchy not suitable (extended by action kind, not by target language)
	@Override
	public String buildAction(STActionBuilder ab, EState curr, EAction a)
	{
		EState succ = curr.getSuccessor(a);
		if (getStateKind(curr) == ParamCoreEStateKind.CROSS_RECEIVE && curr.getActions().size() > 1)
		{
			return
					  "func (" + RPCoreSTApiGenConstants.GO_IO_METHOD_RECEIVER
								+ " *" + ab.getStateChanType(this, curr, a) + ") " + ab.getActionName(this, a) + "(" 
								+ ab.buildArgs(this, a)
								+ ") <-chan *" + ab.getReturnType(this, curr, succ) + " {\n"
					+ ab.buildBody(this, curr, a, succ) + "\n"
					+ "}";
		}
		else
		{
			return
					  "func (" + RPCoreSTApiGenConstants.GO_IO_METHOD_RECEIVER
								+ " *" + ab.getStateChanType(this, curr, a) + ") " + ab.getActionName(this, a) + "(" 
								+ ab.buildArgs(this, a)
								+ ") *" + ab.getReturnType(this, curr, succ) + " {\n"
					+ RPCoreSTApiGenConstants.GO_IO_METHOD_RECEIVER + "." + RPCoreSTApiGenConstants.GO_SCHAN_LINEARRESOURCE
							+ "." + RPCoreSTApiGenConstants.GO_LINEARRESOURCE_USE + "()\n"
					+ ab.buildBody(this, curr, a, succ) + "\n"
					+ "}";
		}
	}

  // FIXME: refactor action builders as interfaces and use generic parameter for kind
	@Override
	public String buildActionReturn(STActionBuilder ab, EState curr, EState succ)
	{
		String sEp = RPCoreSTApiGenConstants.GO_IO_METHOD_RECEIVER + "." + RPCoreSTApiGenConstants.GO_SCHAN_ENDPOINT;
		String res = "";
		res += "return " + getSuccStateChan(ab, curr, succ, sEp);
		return res;
	}

	protected String getSuccStateChan(STActionBuilder ab, EState curr, EState succ, String sEp)
	{
		if (getStateKind(succ) == ParamCoreEStateKind.CROSS_RECEIVE && succ.getActions().size() > 1)
		{
			return RPCoreSTApiGenConstants.GO_IO_METHOD_RECEIVER + "." + RPCoreSTApiGenConstants.GO_SCHAN_ENDPOINT + "."
					+ "NewBranchInit()";  // For branch states (hacky?)  // FIXME: factor out with RPCoreSTSessionApiBuilder and RPCoreSTSelectStateBuilder#getPreamble
					/*+ ((succ.id != this.graph.init.id) ? getStateChanName(succ) : "Init") // cf. ParamCoreSTStateChanApiBuilder::getStateChanPremable init state case
					+ "()";*/
		}
		else
		{
			String res = "&" + ab.getReturnType(this, curr, succ) + "{ " + RPCoreSTApiGenConstants.GO_SCHAN_ENDPOINT + ": " + sEp;
			if (!succ.isTerminal())
			{
				res += ", " + RPCoreSTApiGenConstants.GO_SCHAN_LINEARRESOURCE
								+ ": new(" + RPCoreSTApiGenConstants.GO_LINEARRESOURCE_TYPE + ")";  // FIXME: EndSocket LinearResource special case
			}
			res += " }";
			return res;
		}
	}
	
	@Override
	public String getChannelName(STStateChanApiBuilder api, EAction a)  // Not used?
	{
		throw new RuntimeException("[rp-core] Shouldn't get in here: " + a);
		//return "s.ep.GetChan(s.ep.Proto.(*" + api.gpn.getSimpleName() + ")." + a.peer + ")";
	}
	
	@Override
	protected String makeSTStateName(EState s)
	{
		return (s.isTerminal())
				? makeEndStateName(this.apigen.proto.getSimpleName(), this.variant)
				: (s.id == this.graph.init.id)
						? "Init"  // FIXME: factor out (makeInitStateName)
						: //this.apigen.proto.getSimpleName() + "_" + ParamCoreSTEndpointApiGenerator.getGeneratedActualRoleName(this.actual) + "_"
						  "State"
								+ this.counter++;
	}
	
	public static String makeEndStateName(GProtocolName simpname, RPRoleVariant r)
	{
		return //simpname + "_" + ParamCoreSTEndpointApiGenerator.getGeneratedActualRoleName(r) + "_" + 
				RPCoreSTApiGenConstants.GO_SCHAN_END_TYPE;
	}
	
	// Not actual variants -- rather, indexed roles in EFSM actions -- cf. ParamCoreSTEndpointApiGenerator.getGeneratedRoleVariantName
	public static String getGeneratedIndexedRoleName(RPIndexedRole r) 
	{
		//return r.toString().replaceAll("\\[", "_").replaceAll("\\]", "_").replaceAll("\\.", "_");
		if (r.intervals.size() > 1)
		{
			throw new RuntimeException("[rp-core] TODO: " + r);
		}
		RPInterval g = r.intervals.iterator().next();
		return r.getName() + "_" + g.start + "To" + g.end;
	}
	
	public static String generateIndexExpr(RPIndexExpr e)
	{
		if (e instanceof RPIndexInt)
		{
			return e.toString();
		}
		else if (e instanceof RPIndexVar)
		{
			return RPCoreSTApiGenConstants.GO_IO_METHOD_RECEIVER + "." + RPCoreSTApiGenConstants.GO_SCHAN_ENDPOINT
					+ "." + RPCoreSTApiGenConstants.GO_ENDPOINT_PARAMS + "[\"" + e + "\"]";
		}
		else
		{
			throw new RuntimeException("[param-core] TODO: " + e);
		}
	}

	
	

	protected boolean isDelegType(DataType t)
	{
		return this.dtds.stream().filter(i -> i.getDeclName().equals(t)).iterator().next() instanceof RPCoreDelegDecl;  // FIXME: make a map
	}

	protected String makeExtNameImport(DataType t)
	{
		String extName = getExtName(t);
		switch (extName)
		{
			case "int":	 // FIXME: factor out with batesHack
			case "[]int":	
			case "[][]int":	
			case "string":	
			case "[]string":	
			case "[][]string":	
			case "byte":
			case "[]byte":
			case "[][]byte":
			{
				return "";
			}
			default:
			{
				return "import \"" + getExtSource(t) + "\"\n";
			}
		}
	}
	
	private String getExtName(DataType t)
	{
		return this.dtds.stream().filter(i -> i.getDeclName().equals(t)).iterator().next().extName;  // FIXME: make a map
	}

	private String getExtSource(DataType t)
	{
		return this.dtds.stream().filter(i -> i.getDeclName().equals(t)).iterator().next().extSource;  // FIXME: make a map
	}
	
	// FIXME: deprecate?  array/slice types now handled via ext names?
	protected String batesHack(PayloadElemType<?> t)
	{
		/*String tmp = t.toString();
		return (tmp.equals("bates")) ? "[]byte" : tmp;*/
		DataType dt = (DataType) t;
		String extName = getExtName(dt);
		
		switch (extName)
		{
			case "int":
			case "[]int":  // FIXME: generalise arbitrary dimension array
			case "[][]int":
			case "string":
			case "[]string":
			case "[][]string":
			case "byte":
			case "[]byte":
			case "[][]byte":
			{
				return extName;
			}
			default:
			{
				return extName;  // FIXME
			}
		}
	}
	
	
	
	
	
	
	// FIXME: make a ParamCoreEState
	protected enum ParamCoreEStateKind { CROSS_SEND, CROSS_RECEIVE, DOT_SEND, DOT_RECEIVE, MULTICHOICES_RECEIVE, TERMINAL }
	
	protected static ParamCoreEStateKind getStateKind(EState s)
	{
		List<EAction> as = s.getActions();
		if (as.isEmpty())
		{
			return ParamCoreEStateKind.TERMINAL;	
		}
		else if (as.stream().allMatch(a -> a instanceof RPCoreECrossSend))
		{
			return ParamCoreEStateKind.CROSS_SEND;
		}
		else if (as.stream().allMatch(a -> a instanceof RPCoreECrossReceive))
		{
			return ParamCoreEStateKind.CROSS_RECEIVE;
		}
		/*else if (as.stream().allMatch(a -> a instanceof RPCoreEDotSend))  // FIXME: CFSMs should have only !^1, ! and ?
		{
			return ParamCoreEStateKind.DOT_SEND;
		}
		else if (as.stream().allMatch(a -> a instanceof RPCoreEDotReceive))
		{
			return ParamCoreEStateKind.DOT_RECEIVE;
		}
		else if (as.stream().allMatch(a -> a instanceof RPCoreEMultiChoicesReceive))
		{
			return ParamCoreEStateKind.MULTICHOICES_RECEIVE;
		}*/
		else
		{
			throw new RuntimeException("[rp-core] Shouldn't get in here: " + s);
		}
	}

	
	
	
	
	
	
	
	
	
	
	/*@Override
	public String getPackage()
	{
		//throw new RuntimeException("[rp-core] TODO:");
		return this.gpn.getSimpleName().toString();
	}*/
	
	/*public String getActualRoleName()
	{
		return this.apigen.self.toString();
	}*/
	
	/*protected RPRoleVariant getSelf()
	{
		return (RPRoleVariant) this.getSelf();
	}*/
}

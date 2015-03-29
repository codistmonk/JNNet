package dct;

import static java.util.stream.Collectors.toList;
import static net.sourceforge.aprog.tools.Tools.debugError;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static dct.MiniCAS.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.sourceforge.aprog.tools.Tools;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Printer;

import dct.MiniCAS.Expression;

/**
 * @author codistmonk (creation 2015-03-29)
 */
public final class Expressor extends ClassVisitor {
	
	private final String description;
	
	private final String method;
	
	private Expression result;
	
	public Expressor(final String method, final String description) {
		super(Opcodes.ASM5);
		this.description = description;
		this.method = method;
	}
	
	public final Expression getResult() {
		return this.result;
	}
	
	final void setResult(final Expression result) {
		this.result = result;
	}

	@Override
	public final MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {
		if (this.method.equals(name) && (this.description == null || this.description.equals(desc))) {
			return new MethodVisitor(Opcodes.ASM5) {
				
				private final Map<Integer, Expression> context = new HashMap<>();
				
				private final List<Expression> stack = new ArrayList<>();
				
				private final Map<Integer, String> variableNames = new HashMap<>();
				
				@Override
				public final void visitCode() {
					this.context.clear();
					this.stack.clear();
					this.variableNames.clear();
					setResult(null);
				}
				
				@Override
				public final void visitEnd() {
					final Map<String, Variable> newVariables = new HashMap<>();
					
					this.variableNames.forEach((k, v) -> newVariables.put(this.context.get(k).toString(), variable(v)));
					
					setResult(this.stack.remove(0).accept(new Expression.Visitor<Expression>() {
						
						@Override
						public final Expression visit(final Expression expression) {
							return expression;
						}
						
						@Override
						public final Variable visit(final Variable variable) {
							return newVariables.get(variable.toString());
						}
						
						@Override
						public final UnaryOperation visit(final UnaryOperation operation) {
							final Expression operand = operation.getOperand();
							final Expression newOperand = operand.accept(this);
							
							return operand == newOperand ? operation : operation.newInstance(newOperand);
						}
						
						@Override
						public final CommutativeAssociativeOperation visit(final CommutativeAssociativeOperation operation) {
							final List<Expression> operands = operation.getOperands();
							final List<Expression> newOperands = operands.stream().map(o -> o.accept(this)).collect(toList());
							
							return sameElements(operands, newOperands) ? operation : operation.newInstance(newOperands);
						}
						
						private static final long serialVersionUID = 8473013804645366790L;
						
					}));
					
					
					
					if (!this.stack.isEmpty()) {
						debugError("Invalid state");
						debugError(this.stack);
					}
				}
				
				@Override
				public final void visitLocalVariable(final String name,
						final String desc, final String signature, final Label start,
						final Label end, final int index) {
					this.variableNames.put(index, name);
				}
				
				@Override
				public final void visitMethodInsn(final int opcode, final String owner,
						final String name, final String desc, final boolean itf) {
					switch (opcode) {
					case Opcodes.INVOKESTATIC:
						if ("java/lang/Math".equals(owner)) {
							try {
								this.stack.set(0, (Expression) MiniCAS.class.getMethod(name, Object.class).invoke(null, this.stack.get(0)));
							} catch (final Exception exception) {
								throw unchecked(exception);
							}
							
							break;
						}
					default:
						debugError("Ignoring:", Printer.OPCODES[opcode], owner, name, desc, itf);
						break;
					}
				}
				
				@Override
				public final void visitInsn(final int opcode) {
					switch (opcode) {
					case Opcodes.DCONST_0:
					{
						this.stack.add(0, ZERO);
						break;
					}
					case Opcodes.DCONST_1:
					{
						this.stack.add(0, ONE);
						break;
					}
					case Opcodes.DADD:
					{
						final Expression e = this.stack.remove(0);
						this.stack.set(0, add(this.stack.get(0), e));
						break;
					}
					case Opcodes.DSUB:
					{
						final Expression e = this.stack.remove(0);
						this.stack.set(0, subtract(this.stack.get(0), e));
						break;
					}
					case Opcodes.DMUL:
					{
						final Expression e = this.stack.remove(0);
						this.stack.set(0, multiply(this.stack.get(0), e));
						break;
					}
					case Opcodes.DDIV:
					{
						final Expression e = this.stack.remove(0);
						this.stack.set(0, divide(this.stack.get(0), e));
						break;
					}
					case Opcodes.DRETURN:
						break;
					default:
						debugError("Ignoring:", Printer.OPCODES[opcode]);
						break;
					}
				}
				
				@Override
				public final void visitVarInsn(final int opcode, final int var) {
					switch (opcode) {
					case Opcodes.DLOAD:
						this.stack.add(0, this.context.computeIfAbsent(var, v -> MiniCAS.expression("arg" + this.context.size())));
						
						break;
					case Opcodes.DSTORE:
						this.context.put(var, this.stack.remove(0));
						
						break;
					default:
						debugError("Ignoring:", Printer.OPCODES[opcode], var);
					}
				}
				
				@Override
				public final void visitLdcInsn(final Object cst) {
					this.stack.add(0, expression(cst));
				}
				
				@Override
				public final void visitFieldInsn(final int opcode, final String owner,
						final String name, final String desc) {
					debugError("Ignoring:", Printer.OPCODES[opcode], owner, name, desc);
				}
				
				@Override
				public final void visitInvokeDynamicInsn(final String name,
						final String desc, final Handle bsm, final Object... bsmArgs) {
					debugError("Ignoring:", name, desc, bsm, Arrays.toString(bsmArgs));
				}
				
				@Override
				public final void visitIntInsn(final int opcode, final int operand) {
					debugError("Ignoring:", Printer.OPCODES[opcode]);
				}
				
				@Override
				public final void visitTypeInsn(final int opcode, final String type) {
					debugError("Ignoring:", Printer.OPCODES[opcode], type);
				}
				
				@Override
				public final void visitIincInsn(final int var, final int increment) {
					debugError("Ignoring:", var, "+=", increment);
				}
				
			};
		}
		
		return super.visitMethod(access, name, desc, signature, exceptions);
	}
	
	public static final Expression express(final String className, final String methodName) {
		try {
			final Expressor expressor = new Expressor(methodName, null);
			
			new ClassReader(className).accept(expressor, 0);
			
			return expressor.getResult();
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
}
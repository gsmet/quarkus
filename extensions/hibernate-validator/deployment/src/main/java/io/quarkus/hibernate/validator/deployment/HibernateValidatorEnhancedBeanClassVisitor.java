package io.quarkus.hibernate.validator.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.validator.engine.HibernateValidatorEnhancedBean;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.DescriptorUtils;

public class HibernateValidatorEnhancedBeanClassVisitor extends ClassVisitor {

    private static final String GETTER_PREFIX_GET = "get";
    private static final String GETTER_PREFIX_IS = "is";
    private static final String GETTER_PREFIX_HAS = "has";

    private ClassInfo beanClass;

    private boolean shouldCallSuperMethod;

    public HibernateValidatorEnhancedBeanClassVisitor(ClassVisitor classVisitor, IndexView index, ClassInfo beanClass,
            Set<DotName> classNamesToBeInstrumented) {
        super(Opcodes.ASM7, classVisitor);
        this.beanClass = beanClass;
        this.shouldCallSuperMethod = beanClass.superName() != null
                && classNamesToBeInstrumented.contains(beanClass.superName());
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        String[] newInferfaces = new String[interfaces.length + 1];
        newInferfaces[newInferfaces.length - 1] = DescriptorUtils
                .objectToInternalClassName(HibernateValidatorEnhancedBean.class);
        System.arraycopy(interfaces, 0, newInferfaces, 0, interfaces.length);

        super.visit(version, access, name, signature, superName, newInferfaces);

        addGetFieldValue();
        addGetGetterValue();
    }

    private void addGetFieldValue() {
        MethodVisitor methodVisitor = visitMethod(Modifier.PUBLIC, HibernateValidatorEnhancedBean.GET_FIELD_VALUE_METHOD_NAME,
                "(Ljava/lang/String;)Ljava/lang/Object;", null,
                null);
        methodVisitor.visitCode();

        Label startLabel = new Label();
        methodVisitor.visitLabel(startLabel);

        int index = 0;
        for (FieldInfo field : beanClass.fields()) {
            String fieldName = field.name();

            if (index > 0) {
                methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }

            //      if (propertyName.equals(field_name_goes_here)) {
            //          return field;
            //      }
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitLdcInsn(fieldName);
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    DescriptorUtils.objectToInternalClassName(String.class),
                    "equals",
                    "(Ljava/lang/Object;)Z",
                    false);

            Label ifCheckLabel = new Label();
            methodVisitor.visitJumpInsn(Opcodes.IFEQ, ifCheckLabel);

            Label returnFieldValueLabel = new Label();
            methodVisitor.visitLabel(returnFieldValueLabel);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(
                    Opcodes.GETFIELD,
                    DescriptorUtils.objectToInternalClassName(beanClass.name().toString()),
                    fieldName,
                    DescriptorUtils.typeToString(field.type()));
            if (field.type().kind() == Kind.PRIMITIVE) {
                box(methodVisitor, field.type().asPrimitiveType().primitive());
            }
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitLabel(ifCheckLabel);

            index++;
        }

        // call the super method if the parent class is instrumented
        if (shouldCallSuperMethod) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    DescriptorUtils.objectToInternalClassName(beanClass.superName().toString()),
                    HibernateValidatorEnhancedBean.GET_FIELD_VALUE_METHOD_NAME,
                    "(Ljava/lang/String;)Ljava/lang/Object;",
                    false);
            methodVisitor.visitInsn(Opcodes.ARETURN);
        }

        // throw new IllegalArgumentException("No property was found for the given name");

        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitTypeInsn(Opcodes.NEW, IllegalArgumentException.class.getName().replace('.', '/'));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("No property was found for the given name");
        methodVisitor.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                IllegalArgumentException.class.getName().replace('.', '/'),
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        methodVisitor.visitInsn(Opcodes.ATHROW);

        Label endLabel = new Label();
        methodVisitor.visitLabel(endLabel);
        methodVisitor.visitLocalVariable(
                "this",
                DescriptorUtils.extToInt(beanClass.name().toString()),
                null,
                startLabel,
                endLabel,
                0);
        methodVisitor.visitLocalVariable(
                "name",
                "Ljava/lang/String;",
                null,
                startLabel,
                endLabel,
                1);
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    private void addGetGetterValue() {
        MethodVisitor methodVisitor = visitMethod(Modifier.PUBLIC, HibernateValidatorEnhancedBean.GET_GETTER_VALUE_METHOD_NAME,
                "(Ljava/lang/String;)Ljava/lang/Object;", null,
                null);
        methodVisitor.visitCode();

        Label startLabel = new Label();
        methodVisitor.visitLabel(startLabel);

        int index = 0;
        for (MethodInfo method : getGetterMethods(beanClass)) {
            String methodName = method.name();

            if (index > 0) {
                methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }

            //      if (propertyName.equals(field_name_goes_here)) {
            //          return field;
            //      }
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitLdcInsn(methodName);
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    DescriptorUtils.objectToInternalClassName(String.class),
                    "equals",
                    "(Ljava/lang/Object;)Z",
                    false);

            Label ifCheckLabel = new Label();
            methodVisitor.visitJumpInsn(Opcodes.IFEQ, ifCheckLabel);

            Label returnGetterValueLabel = new Label();
            methodVisitor.visitLabel(returnGetterValueLabel);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);

            String[] paramTypes = new String[method.parameters().size()];
            for (int i = 0; i < paramTypes.length; ++i) {
                paramTypes[i] = DescriptorUtils.typeToString(method.parameters().get(i));
            }

            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    DescriptorUtils.objectToInternalClassName(beanClass.name().toString()),
                    method.name(),
                    DescriptorUtils.methodSignatureToDescriptor(DescriptorUtils.typeToString(method.returnType()),
                            paramTypes),
                    false);
            if (method.returnType().kind() == Kind.PRIMITIVE) {
                box(methodVisitor, method.returnType().asPrimitiveType().primitive());
            }
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitLabel(ifCheckLabel);

            index++;
        }

        // call the super method if the parent class is instrumented
        if (shouldCallSuperMethod) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    DescriptorUtils.objectToInternalClassName(beanClass.superName().toString()),
                    HibernateValidatorEnhancedBean.GET_GETTER_VALUE_METHOD_NAME,
                    "(Ljava/lang/String;)Ljava/lang/Object;",
                    false);
            methodVisitor.visitInsn(Opcodes.ARETURN);
        }

        // throw new IllegalArgumentException("No property was found for the given name");

        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitTypeInsn(Opcodes.NEW, IllegalArgumentException.class.getName().replace('.', '/'));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("No property was found for the given name");
        methodVisitor.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                IllegalArgumentException.class.getName().replace('.', '/'),
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        methodVisitor.visitInsn(Opcodes.ATHROW);

        Label endLabel = new Label();
        methodVisitor.visitLabel(endLabel);
        methodVisitor.visitLocalVariable(
                "this",
                DescriptorUtils.extToInt(beanClass.name().toString()),
                null,
                startLabel,
                endLabel,
                0);
        methodVisitor.visitLocalVariable(
                "name",
                "Ljava/lang/String;",
                null,
                startLabel,
                endLabel,
                1);
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    private static List<MethodInfo> getGetterMethods(ClassInfo clazz) {
        List<MethodInfo> getterMethods = new ArrayList<>();
        for (MethodInfo method : clazz.methods()) {
            Type returnType = method.returnType();
            String name = method.name();

            if (!method.parameters().isEmpty() || returnType.kind() == Kind.VOID) {
                continue;
            }

            if ((name.startsWith(GETTER_PREFIX_GET)) ||
                    (name.startsWith(GETTER_PREFIX_IS) && returnType.kind() == Kind.PRIMITIVE
                            && returnType.asPrimitiveType().primitive() == Primitive.BOOLEAN)
                    ||
                    (name.startsWith(GETTER_PREFIX_HAS) && returnType.kind() == Kind.PRIMITIVE
                            && returnType.asPrimitiveType().primitive() == Primitive.BOOLEAN)) {
                getterMethods.add(method);
            }
        }
        return getterMethods;
    }

    private static void box(MethodVisitor mv, Primitive primitive) {
        switch (primitive) {
            case BOOLEAN:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case BYTE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case CHAR:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case SHORT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case INT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case FLOAT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case LONG:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case DOUBLE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
        }
    }
}

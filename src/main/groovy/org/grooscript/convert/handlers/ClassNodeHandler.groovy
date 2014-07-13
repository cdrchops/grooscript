package org.grooscript.convert.handlers

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.BlockStatement

import static org.grooscript.JsNames.*
/**
 * User: jorgefrancoleza
 * Date: 16/01/14
 */
class ClassNodeHandler extends BaseHandler {

    void handle(ClassNode node) {
        //Exit if dont have to convert
        if (haveAnnotationNonConvert(node.annotations)) {
            return
        }

        out.addLine()

        //Push name in stack
        context.classNameStack.push(node.nameWithoutPackage)
        context.variableScoping.push([])
        context.variableStaticScoping.push([])

        out.block ("function ${node.nameWithoutPackage}() ") {

            //Limited allowed inheritance
            context.superNameStack.push(node.superClass.name)

            if (node.superClass.name != 'java.lang.Object') {
                //println 'Allowed!'+ node.superClass.class.name
                out.addScript("var ${GS_OBJECT} = ${node.superClass.nameWithoutPackage}();", true)

                //We add to this class scope variables of fathers
                addSuperClassesToScope(node.superClass)
            } else {
                out.addScript("var ${GS_OBJECT} = ${GS_INHERIT}(${GS_BASE_CLASS},'${node.nameWithoutPackage}');", true)
            }

            //Class names and interfaces
            putClassNamesAndInterfaces(node)

            //Add variable names to scope
            addClassVariableNamesToScope(node)

            //Traits
            checkTraits(node)

            //Adding initial values of properties
            node?.properties?.each { PropertyNode property ->
                if (!property.isStatic()) {
                    addPropertyToClass(property, false)
                    //We add variable names of the class
                } else {
                    addPropertyStaticToClass(property.name)
                }
            }

            //Add fields not added as properties
            node.fields.each { FieldNode field ->
                checkDelegateAnnotation(field, node.nameWithoutPackage)

                if (field.owner.name == node.name && (field.isPublic()|| !node.properties.any { it.name == field.name})) {
                    if (!field.isStatic()) {
                        addPropertyToClass(field,false)
                    } else {
                        addPropertyStaticToClass(field.name)
                    }
                }
            }

            processClassMethods(node?.methods, node)

            //Constructors
            checkConstructors(node)

            //@Mixin
            checkAddMixin(node.nameWithoutPackage, node.annotations)

            out.addLine()
            out.addScript("return ${GS_OBJECT};")
        }

        //Static methods
        node?.methods?.each { MethodNode method ->
            context.staticProcessNode = node
            if (!haveAnnotationNonConvert(method.annotations)) {
                if (method.isStatic()) {
                    if (functions.haveAnnotationNative(method.annotations)) {
                        functions.putGsNativeMethod("${node.nameWithoutPackage}.${method.name}", node, method)
                    } else {
                        functions.processBasicFunction("${node.nameWithoutPackage}.${method.name}", method, false)
                    }
                }
            }
            context.staticProcessNode = null
        }

        //Static properties
        node?.properties?.each { it-> //println 'Property->'+it; println 'initialExpresion->'+it.initialExpression
            if (it.isStatic()) {
                out.addScript(node.nameWithoutPackage)
                addPropertyToClass(it, true)
            }
        }

        //Remove variable class names from the list
        context.variableScoping.pop()
        context.variableStaticScoping.pop()

        //Pop name in stack
        context.classNameStack.pop()
        context.superNameStack.pop()

        //@Category
        checkAddCategory(node.nameWithoutPackage, node.annotations)
    }

    private processClassMethods(List<MethodNode> methods, ClassNode classNode) {

        context.processingClassMethods = true
        methods?.each { MethodNode methodNode ->
            if (!haveAnnotationNonConvert(methodNode.annotations) && !methodNode.isAbstract()) {
                //Process the methods
                if (functions.haveAnnotationNative(methodNode.annotations) && !methodNode.isStatic()) {
                    functions.putGsNativeMethod("${GS_OBJECT}.${methodNode.name}", classNode, methodNode)
                } else if (!methodNode.isStatic()) {
                    if (methodNode.name == 'propertyMissing' && methodNode.parameters.length == 2) {
                        functions.processBasicFunction("${GS_OBJECT}['setPropertyMissing']", methodNode, false)
                    } else {
                        factory.visitNode(methodNode, false)
                    }
                } else {
                    staticMethod(methodNode, GS_OBJECT, classNode.nameWithoutPackage)
                }
            }
        }
        context.processingClassMethods = false
    }

    private staticMethod(MethodNode methodNode, String objectName, String nodeName, boolean withSelf = false) {
        //We put the number of params as x? name variables
        def numberParams = 0
        if (methodNode.parameters && methodNode.parameters.size()>0) {
            numberParams = methodNode.parameters.size()
        }
        def params = []
        def paramsCall = []
        numberParams.times { number ->
            params << 'x'+number
            paramsCall << 'x'+number
        }
        if (withSelf) {
            params.remove(0)
            paramsCall[0] = objectName
        }

        out.addScript("${objectName}.${methodNode.name} = function(${params.join(',')}) { return ${nodeName}.${methodNode.name}(")
        out.addScript(paramsCall.join(','))
        out.addScript("); }", true)
    }

    private haveAnnotationNonConvert(annotations) {
        boolean exit = false
        annotations.each { AnnotationNode it ->
            //If dont have to convert then exit
            if (it.getClassNode().nameWithoutPackage=='GsNotConvert') {
                exit = true
            }
        }
        return exit
    }

    private haveAnnotationGroovyImmutable(annotations) {
        boolean exit = false
        annotations.each { AnnotationNode it ->
            if (it.getClassNode().name == 'groovy.transform.Immutable') {
                exit = true
            }
        }
        exit
    }

    private checkConstructors(ClassNode node) {

        boolean has1parameterConstructor = false
        node?.declaredConstructors?.each { MethodNode it->
            def numberArguments = it.parameters?.size()
            if (numberArguments == 1) {
                has1parameterConstructor = true
            }
            factory.visitNode(it, true)

            addConditionConstructorExecution(numberArguments, it.parameters)
        }

        if (haveAnnotationGroovyImmutable(node.annotations)) {
            //Add a constructor with params
            def paramSize = node.properties.size()
            def paramNames = node.properties.collect { it.name }.join(', ')
            def nameFunction = "${GS_OBJECT}.${node.nameWithoutPackage}${paramSize}"
            out.addScript("${nameFunction} = function(${paramNames}) {")
            node.properties.collect { it.name }.each {
                out.addScript("  ${GS_OBJECT}.${it} = ${it}; ")
            }
            out.addScript("  return this; ")
            out.addScript("};", true)
            out.addScript("if (arguments.length==${paramSize}) {" +
                    "${nameFunction}.apply(${GS_OBJECT}, arguments); }", true)
            if (paramSize == 1) {
                has1parameterConstructor = true
            }
        }

        //If no constructor with 1 parameter, we create 1 that get a map, for put value on properties
        if (!has1parameterConstructor) {
            out.addScript("if (arguments.length == 1) {" +
                    "${GS_PASS_MAP_TO_OBJECT}(arguments[0],${GS_OBJECT});};", true)
        }
    }

    private addPropertyToClass(fieldOrProperty,isStatic) {

        def previous = GS_OBJECT
        if (isStatic) {
            previous = ''
        }

        if (fieldOrProperty.initialExpression) {
            out.addScript("${previous}.${fieldOrProperty.name} = ")
            factory.visitNode(fieldOrProperty.initialExpression)
            out.addScript(';', true)
        } else {
            out.addScript("${previous}.${fieldOrProperty.name} = null;", true)
        }
    }

    private addPropertyStaticToClass(String name) {

        out.addScript("${GS_OBJECT}.__defineGetter__('${name}', function(){ " +
                "return ${context.classNameStack.peek()}.${name}; });", true)
        out.addScript("${GS_OBJECT}.__defineSetter__('${name}', function(${VALUE}){ " +
                "${context.classNameStack.peek()}.${name} = ${VALUE}; });", true)
    }

    private checkAddMixin(className, annotations) {

        annotations.each { AnnotationNode annotationNode ->
            if (annotationNode.getClassNode().name == 'groovy.lang.Mixin') {
                def list = []
                annotationNode.members.values().each { value ->
                    if (value instanceof ListExpression) {
                        value.expressions.each { it ->
                            list << it.type.nameWithoutPackage
                        }
                    } else {
                        list << value.type.nameWithoutPackage
                    }
                }
                addMixinToClass(className, list)
            }
        }
    }

    private addMixinToClass(className, List<String> listMixins) {
        out.addScript("${GS_MIXIN_CLASS}('${className}',")
        out.addScript('[')
        out.addScript listMixins.join(',')
        out.addScript(']);', true)
    }

    private checkAddCategory(className, annotations) {
        annotations.each { AnnotationNode annotationNode ->
            if (annotationNode.getClassNode().name == 'groovy.lang.Category') {
                annotationNode.members.values().each { value ->
                    if (value instanceof ListExpression) {
                        value.expressions.each { it ->
                            addCategoryToClass(className, it.type.nameWithoutPackage)
                        }
                    } else {
                        addCategoryToClass(className, value.type.nameWithoutPackage)
                    }
                }
                out.addScript("${GS_MY_CATEGORIES}['${className}'] = ${className};", true)
            }
        }
    }

    private addCategoryToClass(categoryName, className) {
        out.addScript("${GS_ADD_CATEGORY_ANNOTATION}('${categoryName}','${className}');", true)
    }

    /**
     * Create code the js class definition, for execute constructor
     * @param numberArguments
     * @param paramList
     * @return
     */
    private addConditionConstructorExecution(numberArguments, paramList) {

        out.addScript("if (arguments.length==${numberArguments}) {")
        out.addScript("${GS_OBJECT}.${context.classNameStack.peek()}${numberArguments}")

        out.addScript '('
        def count = 0
        paramList?.each { param ->
            if (count>0) out.addScript ', '
            out.addScript("arguments[${count}]")
            count++
        }
        out.addScript ')'

        out.addScript('; }', true)
    }

    private addSuperClassesToScope(ClassNode node) {
        addClassVariableNamesToScope(node)
        if (node.superClass.isPrimaryClassNode() && node.superClass.name != 'java.lang.Object') {
            addSuperClassesToScope(node.superClass)
        }
    }

    private addClassVariableNamesToScope(ClassNode node) {
        node?.properties?.each { PropertyNode property ->
            if (!property.isStatic()) {
                context.variableScoping.peek().add(property.name)
            } else {
                context.variableStaticScoping.peek().add(property.name);
            }
        }
        node.fields.each { FieldNode field ->
            if (field.owner.name == node.name && (field.isPublic()|| !node.properties.any { it.name == field.name})) {
                if (!field.isStatic()) {
                    context.variableScoping.peek().add(field.name)
                } else {
                    context.variableStaticScoping.peek().add(field.name)
                }
            }
        }
        node.methods?.each { MethodNode it ->
            //Add method names to variable scoping
            if (!it.isStatic() && !it.isAbstract()) {
                context.variableScoping.peek().add(it.name)
            }
        }
    }

    private checkDelegateAnnotation(FieldNode fieldNode, String nameClass) {
        if (fieldHasDelegateAnnotation(fieldNode)) {
            out.addScript("$GS_AST_DELEGATE('${nameClass}', '${fieldNode.name}');", true)
        }
    }

    private boolean fieldHasDelegateAnnotation(FieldNode fieldNode) {
        fieldNode.annotations.any { annotationNode ->
            annotationNode.getClassNode().name == 'groovy.lang.Delegate'
        }
    }

    private checkTraits(ClassNode classNode) {
        classNode.interfaces.findAll {
            traits.isTrait(it)
        }.each {
            handleTrait(it)
        }
    }

    private handleTrait(ClassNode classNode, notAddThisMethods = []) {
        addClassVariableNamesToScope(classNode)
        ClassNode helperClassNode = org.codehaus.groovy.transform.trait.Traits.findHelpers(classNode).helper
        helperClassNode.outerClass.interfaces?.findAll{ traits.isTrait(it) }.each { ClassNode cn ->
            handleTrait(cn, notAddThisMethods + helperClassNode.methods*.name)
        }
        addTraitMethods(classNode, helperClassNode, notAddThisMethods)
    }

    private addTraitMethods(ClassNode classNode, ClassNode helperClassNode, notAddThisMethods) {
        helperClassNode.methods.each {
            if (it.name == '$init$') {
                if (it.code instanceof BlockStatement && !it.code.isEmpty()) {
                    out.addScript("${classNode.nameWithoutPackage}.\$init\$(${GS_OBJECT});", true)
                }
            } else if (it.name == '$static$init$') {

            } else {
                if (!(it.name in notAddThisMethods)) {
                    staticMethod(it, GS_OBJECT, classNode.nameWithoutPackage, true)
                }
            }
        }
    }

    private putClassNamesAndInterfaces(ClassNode node) {
        out.addScript("${GS_OBJECT}.${CLASS} = ${jsObjectNames(node)};", true)
        if (node.superClass) {
            out.addScript("${GS_OBJECT}.${CLASS}.superclass = ${jsObjectNames(node.superClass)};", true)
        }
        if (node.interfaces) {
            out.addScript("${GS_OBJECT}.${CLASS}.interfaces = [")
            node.interfaces.each { ClassNode interfaz ->
                writeInterface(interfaz)
            }
            out.addScript('];', true)
        }
    }

    private jsObjectNames(ClassNode node) {
        "{ name: '${node.name}', simpleName: '${node.nameWithoutPackage}'}"
    }

    private writeInterface(ClassNode classNode) {
        out.addScript(jsObjectNames(classNode) + ', ')
        classNode.interfaces.each {
            writeInterface(it)
        }
    }
}
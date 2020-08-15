// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.generator.gapic.composer;

import com.google.api.core.BetaApi;
import com.google.api.generator.engine.ast.AnnotationNode;
import com.google.api.generator.engine.ast.AssignmentExpr;
import com.google.api.generator.engine.ast.ClassDefinition;
import com.google.api.generator.engine.ast.ConcreteReference;
import com.google.api.generator.engine.ast.Expr;
import com.google.api.generator.engine.ast.ExprStatement;
import com.google.api.generator.engine.ast.ForStatement;
import com.google.api.generator.engine.ast.IfStatement;
import com.google.api.generator.engine.ast.MethodDefinition;
import com.google.api.generator.engine.ast.MethodInvocationExpr;
import com.google.api.generator.engine.ast.NewObjectExpr;
import com.google.api.generator.engine.ast.NullObjectValue;
import com.google.api.generator.engine.ast.Reference;
import com.google.api.generator.engine.ast.ReturnExpr;
import com.google.api.generator.engine.ast.ScopeNode;
import com.google.api.generator.engine.ast.Statement;
import com.google.api.generator.engine.ast.StringObjectValue;
import com.google.api.generator.engine.ast.ThisObjectValue;
import com.google.api.generator.engine.ast.ThrowExpr;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.ValueExpr;
import com.google.api.generator.engine.ast.VaporReference;
import com.google.api.generator.engine.ast.Variable;
import com.google.api.generator.engine.ast.VariableExpr;
import com.google.api.generator.gapic.model.GapicClass;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.utils.JavaStyle;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.pathtemplate.ValidationException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Generated;

public class ResourceNameHelperClassComposer {
  private static final String CLASS_NAME_PATTERN = "%sName";
  private static final String SLASH = "/";
  private static final String LEFT_BRACE = "{";
  private static final String RIGHT_BRACE = "}";

  private static final ResourceNameHelperClassComposer INSTANCE =
      new ResourceNameHelperClassComposer();

  private static final Map<String, TypeNode> STATIC_TYPES = createStaticTypes();
  private static final Map<String, VariableExpr> FIXED_CLASS_VARS =
      createFixedClassMemberVariables();

  private ResourceNameHelperClassComposer() {}

  public static ResourceNameHelperClassComposer instance() {
    return INSTANCE;
  }

  public GapicClass generate(ResourceName resourceName) {
    List<List<String>> tokenHierarchies = parseTokenHierarchy(resourceName.patterns());
    Map<String, TypeNode> types = createDynamicTypes(resourceName, tokenHierarchies);
    List<VariableExpr> templateFinalVarExprs = createTemplateClassMembers(tokenHierarchies);
    Map<String, VariableExpr> patternTokenVarExprs =
        createPatternTokenClassMembers(tokenHierarchies);

    String className = getThisClassName(resourceName);

    ClassDefinition classDef =
        ClassDefinition.builder()
            .setPackageString(resourceName.pakkage())
            .setAnnotations(createClassAnnotations())
            .setScope(ScopeNode.PUBLIC)
            .setName(className)
            .setImplementsTypes(createImplementsTypes())
            .setStatements(
                createClassStatements(
                    templateFinalVarExprs,
                    patternTokenVarExprs,
                    resourceName.patterns(),
                    tokenHierarchies))
            .setMethods(
                createClassMethods(
                    resourceName,
                    templateFinalVarExprs,
                    patternTokenVarExprs,
                    tokenHierarchies,
                    types))
            .build();
    return GapicClass.create(GapicClass.Kind.PROTO, classDef);
  }

  private static List<AnnotationNode> createClassAnnotations() {
    return Arrays.asList(
        AnnotationNode.builder()
            .setType(STATIC_TYPES.get("Generated"))
            .setDescription("by gapic-generator-java")
            .build());
  }

  private static List<TypeNode> createImplementsTypes() {
    return Arrays.asList(STATIC_TYPES.get("ResourceName"));
  }

  private static List<VariableExpr> createTemplateClassMembers(
      List<List<String>> tokenHierarchies) {
    return tokenHierarchies.stream()
        .map(
            ts ->
                VariableExpr.withVariable(
                    Variable.builder()
                        .setName(concatToUpperSnakeCaseName(ts))
                        .setType(STATIC_TYPES.get("PathTemplate"))
                        .build()))
        .collect(Collectors.toList());
  }

  private static Map<String, VariableExpr> createPatternTokenClassMembers(
      List<List<String>> tokenHierarchies) {
    Set<String> tokenSet = getTokenSet(tokenHierarchies);
    return tokenSet.stream()
        .map(
            t ->
                VariableExpr.withVariable(
                    Variable.builder().setName(t).setType(TypeNode.STRING).build()))
        .collect(Collectors.toMap(v -> v.variable().identifier().name(), v -> v));
  }

  private static List<Statement> createClassStatements(
      List<VariableExpr> templateFinalVarExprs,
      Map<String, VariableExpr> patternTokenVarExprs,
      List<String> patterns,
      List<List<String>> tokenHierarchies) {
    List<Expr> memberVars = new ArrayList<>();
    Preconditions.checkState(
        templateFinalVarExprs.size() == patterns.size()
            && tokenHierarchies.size() == patterns.size(),
        "Cardinalities of patterns and associated variables do not match");
    // Pattern string variables.
    // Example:
    // private static final PathTemplate PROJECT_LOCATION_AUTOSCALING_POLICY_PATH_TEMPLATE =
    //     PathTemplate.createWithoutUrlEncoding(
    //         "projects/{project}/locations/{location}/autoscalingPolicies/{autoscaling_policy}");
    for (int i = 0; i < patterns.size(); i++) {
      VariableExpr varExpr =
          templateFinalVarExprs.get(i).toBuilder()
              .setIsDecl(true)
              .setScope(ScopeNode.PRIVATE)
              .setIsStatic(true)
              .setIsFinal(true)
              .build();
      Expr createWithoutUrlEncodingExpr =
          MethodInvocationExpr.builder()
              .setStaticReferenceType(STATIC_TYPES.get("PathTemplate"))
              .setMethodName("createWithoutUrlEncoding")
              .setArguments(
                  Arrays.asList(ValueExpr.withValue(StringObjectValue.withValue(patterns.get(i)))))
              .setReturnType(STATIC_TYPES.get("PathTemplate"))
              .build();
      memberVars.add(
          AssignmentExpr.builder()
              .setVariableExpr(varExpr)
              .setValueExpr(createWithoutUrlEncodingExpr)
              .build());
    }

    memberVars.add(
        FIXED_CLASS_VARS.get("fieldValuesMap").toBuilder()
            .setIsDecl(true)
            .setScope(ScopeNode.PRIVATE)
            .setIsVolatile(true)
            .build());
    Function<VariableExpr, VariableExpr> toDeclFn =
        v -> v.toBuilder().setIsDecl(true).setScope(ScopeNode.PRIVATE).build();
    memberVars.add(toDeclFn.apply(FIXED_CLASS_VARS.get("pathTemplate")));
    memberVars.add(toDeclFn.apply(FIXED_CLASS_VARS.get("fixedValue")));

    // Private per-token string variables.
    // Use the token set as a key to maintain ordering (for consistency).
    memberVars.addAll(
        getTokenSet(tokenHierarchies).stream()
            .map(t -> toDeclFn.apply(patternTokenVarExprs.get(t)))
            .collect(Collectors.toList()));
    return memberVars.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList());
  }

  private static List<MethodDefinition> createClassMethods(
      ResourceName resourceName,
      List<VariableExpr> templateFinalVarExprs,
      Map<String, VariableExpr> patternTokenVarExprs,
      List<List<String>> tokenHierarchies,
      Map<String, TypeNode> types) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    javaMethods.addAll(
        createConstructorMethods(
            resourceName, templateFinalVarExprs, patternTokenVarExprs, tokenHierarchies, types));
    javaMethods.addAll(createTokenGetterMethods(patternTokenVarExprs, tokenHierarchies));
    javaMethods.addAll(createBuilderCreatorMethods(resourceName, tokenHierarchies, types));
    javaMethods.addAll(
        createOfCreatorMethods(resourceName, patternTokenVarExprs, tokenHierarchies, types));
    javaMethods.addAll(
        createFormatCreatorMethods(resourceName, patternTokenVarExprs, tokenHierarchies, types));
    javaMethods.addAll(
        createParsingAndSplittingMethods(
            resourceName, templateFinalVarExprs, tokenHierarchies, types));

    return javaMethods;
  }

  private static List<MethodDefinition> createConstructorMethods(
      ResourceName resourceName,
      List<VariableExpr> templateFinalVarExprs,
      Map<String, VariableExpr> patternTokenVarExprs,
      List<List<String>> tokenHierarchies,
      Map<String, TypeNode> types) {
    String thisClassName = getThisClassName(resourceName);
    TypeNode thisClassType = types.get(thisClassName);

    List<MethodDefinition> javaMethods = new ArrayList<>();
    MethodDefinition deprecatedCtor =
        MethodDefinition.constructorBuilder()
            .setScope(ScopeNode.PROTECTED)
            .setAnnotations(
                Arrays.asList(
                    AnnotationNode.withType(
                        TypeNode.withReference(ConcreteReference.withClazz(Deprecated.class)))))
            .setReturnType(thisClassType)
            .build();
    javaMethods.add(deprecatedCtor);

    for (int i = 0; i < tokenHierarchies.size(); i++) {
      List<String> tokens = tokenHierarchies.get(i);
      List<Expr> bodyExprs = new ArrayList<>();
      TypeNode argType = getBuilderType(types, tokenHierarchies, i);
      VariableExpr builderArgExpr =
          VariableExpr.withVariable(Variable.builder().setName("builder").setType(argType).build());
      for (String token : tokens) {
        MethodInvocationExpr checkNotNullExpr =
            MethodInvocationExpr.builder()
                .setStaticReferenceType(STATIC_TYPES.get("Preconditions"))
                .setMethodName("checkNotNull")
                .setReturnType(TypeNode.STRING)
                .setArguments(
                    Arrays.asList(
                        MethodInvocationExpr.builder()
                            .setExprReferenceExpr(builderArgExpr)
                            .setMethodName(
                                String.format("get%s", JavaStyle.toUpperCamelCase(token)))
                            .build()))
                .build();
        bodyExprs.add(
            AssignmentExpr.builder()
                .setVariableExpr(patternTokenVarExprs.get(token))
                .setValueExpr(checkNotNullExpr)
                .build());
      }
      AssignmentExpr pathTemplateAssignExpr =
          AssignmentExpr.builder()
              .setVariableExpr(FIXED_CLASS_VARS.get("pathTemplate"))
              .setValueExpr(templateFinalVarExprs.get(i))
              .build();
      bodyExprs.add(pathTemplateAssignExpr);
      javaMethods.add(
          MethodDefinition.constructorBuilder()
              .setScope(ScopeNode.PRIVATE)
              .setReturnType(thisClassType)
              .setArguments(Arrays.asList(builderArgExpr.toBuilder().setIsDecl(true).build()))
              .setBody(
                  bodyExprs.stream()
                      .map(e -> ExprStatement.withExpr(e))
                      .collect(Collectors.toList()))
              .build());
    }

    return javaMethods;
  }

  private static List<MethodDefinition> createTokenGetterMethods(
      Map<String, VariableExpr> patternTokenVarExprs, List<List<String>> tokenHierarchies) {
    return getTokenSet(tokenHierarchies).stream()
        .map(
            t ->
                MethodDefinition.builder()
                    .setScope(ScopeNode.PUBLIC)
                    .setReturnType(TypeNode.STRING)
                    .setName(String.format("get%s", JavaStyle.toUpperCamelCase(t)))
                    .setReturnExpr(patternTokenVarExprs.get(t))
                    .build())
        .collect(Collectors.toList());
  }

  private static List<MethodDefinition> createBuilderCreatorMethods(
      ResourceName resourceName, List<List<String>> tokenHierarchies, Map<String, TypeNode> types) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    String newMethodNameFormat = "new%s";
    AnnotationNode betaAnnotation =
        AnnotationNode.builder()
            .setType(STATIC_TYPES.get("BetaApi"))
            .setDescription(
                "The per-pattern Builders are not stable yet and may be changed in the future.")
            .build();
    List<AnnotationNode> annotations = Arrays.asList(betaAnnotation);

    // Create the newBuilder and variation methods here.
    // Variation example: newProjectLocationAutoscalingPolicyBuilder().
    for (int i = 0; i < tokenHierarchies.size(); i++) {
      final TypeNode returnType = getBuilderType(types, tokenHierarchies, i);
      final Expr returnExpr = NewObjectExpr.withType(returnType);

      Function<String, MethodDefinition.Builder> methodDefStarterFn =
          methodName ->
              MethodDefinition.builder()
                  .setScope(ScopeNode.PUBLIC)
                  .setIsStatic(true)
                  .setReturnType(returnType)
                  .setName(methodName)
                  .setReturnExpr(returnExpr);

      String variantName = getBuilderTypeName(tokenHierarchies, i);
      javaMethods.add(
          methodDefStarterFn
              .apply(String.format(newMethodNameFormat, variantName))
              .setAnnotations(i == 0 ? Collections.emptyList() : annotations)
              .build());
      if (i == 0 && tokenHierarchies.size() > 1) {
        // Create another builder creator method, but with the per-variant name.
        javaMethods.add(
            methodDefStarterFn
                .apply(
                    String.format(newMethodNameFormat, getBuilderTypeName(tokenHierarchies.get(i))))
                .setAnnotations(annotations)
                .build());
      }
    }

    // TODO(miraleung, v2): It seems weird that we currently generate a toBuilder method only for
    // the default class, and none for the Builder variants.
    TypeNode toBuilderReturnType = getBuilderType(types, tokenHierarchies, 0);
    TypeNode thisClassType = types.get(getThisClassName(resourceName));
    javaMethods.add(
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setReturnType(toBuilderReturnType)
            .setName("toBuilder")
            .setReturnExpr(
                NewObjectExpr.builder()
                    .setType(toBuilderReturnType)
                    .setArguments(
                        Arrays.asList(ValueExpr.withValue(ThisObjectValue.withType(thisClassType))))
                    .build())
            .build());
    return javaMethods;
  }

  private static List<MethodDefinition> createOfCreatorMethods(
      ResourceName resourceName,
      Map<String, VariableExpr> patternTokenVarExprs,
      List<List<String>> tokenHierarchies,
      Map<String, TypeNode> types) {
    return createOfOrFormatMethodHelper(
        resourceName, patternTokenVarExprs, tokenHierarchies, types, /*isFormatMethod=*/ false);
  }

  private static List<MethodDefinition> createFormatCreatorMethods(
      ResourceName resourceName,
      Map<String, VariableExpr> patternTokenVarExprs,
      List<List<String>> tokenHierarchies,
      Map<String, TypeNode> types) {
    return createOfOrFormatMethodHelper(
        resourceName, patternTokenVarExprs, tokenHierarchies, types, /*isFormatMethod=*/ true);
  }

  private static List<MethodDefinition> createOfOrFormatMethodHelper(
      ResourceName resourceName,
      Map<String, VariableExpr> patternTokenVarExprs,
      List<List<String>> tokenHierarchies,
      Map<String, TypeNode> types,
      boolean isFormatMethod) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    String methodNameFormat = isFormatMethod ? "format%s" : "of%s";
    String newBuilderMethodNameFormat = "new%s";
    String setMethodNameFormat = "set%s";
    String buildMethodName = "build";
    String toStringMethodName = "toString";
    AnnotationNode betaAnnotation =
        AnnotationNode.builder()
            .setType(STATIC_TYPES.get("BetaApi"))
            .setDescription(
                String.format(
                    "The static %s methods are not stable yet and may be changed in the future.",
                    isFormatMethod ? "format" : "create"))
            .build();
    List<AnnotationNode> annotations = Arrays.asList(betaAnnotation);

    TypeNode thisClassType = types.get(getThisClassName(resourceName));
    TypeNode returnType = isFormatMethod ? TypeNode.STRING : thisClassType;
    // Create the newBuilder and variation methods here.
    // Variation example: newProjectLocationAutoscalingPolicyBuilder().
    for (int i = 0; i < tokenHierarchies.size(); i++) {
      List<String> tokens = tokenHierarchies.get(i);
      String builderMethodName =
          String.format(newBuilderMethodNameFormat, getBuilderTypeName(tokens));

      MethodInvocationExpr returnExpr =
          MethodInvocationExpr.builder().setMethodName(builderMethodName).build();
      for (String token : tokens) {
        returnExpr =
            MethodInvocationExpr.builder()
                .setExprReferenceExpr(returnExpr)
                .setMethodName(
                    String.format(setMethodNameFormat, JavaStyle.toUpperCamelCase(token)))
                .setArguments(
                    Arrays.asList(
                        VariableExpr.withVariable(
                            Variable.builder().setName(token).setType(TypeNode.STRING).build())))
                .build();
      }
      returnExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(returnExpr)
              .setMethodName(buildMethodName)
              .setReturnType(thisClassType)
              .build();
      if (isFormatMethod) {
        returnExpr =
            MethodInvocationExpr.builder()
                .setExprReferenceExpr(returnExpr)
                .setMethodName(toStringMethodName)
                .setReturnType(TypeNode.STRING)
                .build();
      }
      List<VariableExpr> methodArgs =
          tokens.stream()
              .map(t -> patternTokenVarExprs.get(t).toBuilder().setIsDecl(true).build())
              .collect(Collectors.toList());
      javaMethods.add(
          MethodDefinition.builder()
              .setScope(ScopeNode.PUBLIC)
              .setIsStatic(true)
              .setAnnotations(i == 0 ? Collections.emptyList() : annotations)
              .setReturnType(returnType)
              .setName(
                  String.format(
                      methodNameFormat, i == 0 ? "" : getBuilderTypeName(tokenHierarchies, i)))
              .setArguments(methodArgs)
              .setReturnExpr(returnExpr)
              .build());

      if (i == 0 && tokenHierarchies.size() > 1) {
        javaMethods.add(
            MethodDefinition.builder()
                .setScope(ScopeNode.PUBLIC)
                .setIsStatic(true)
                .setAnnotations(annotations)
                .setReturnType(returnType)
                .setName(String.format(methodNameFormat, getBuilderTypeName(tokens)))
                .setArguments(methodArgs)
                .setReturnExpr(returnExpr)
                .build());
      }
    }
    return javaMethods;
  }

  private static List<MethodDefinition> createParsingAndSplittingMethods(
      ResourceName resourceName,
      List<VariableExpr> templateFinalVarExprs,
      List<List<String>> tokenHierarchies,
      Map<String, TypeNode> types) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    TypeNode thisClassType = types.get(getThisClassName(resourceName));
    javaMethods.add(
        createParseMethod(thisClassType, templateFinalVarExprs, tokenHierarchies, types));
    javaMethods.add(createParseListMethod(thisClassType));
    javaMethods.add(createToStringListMethod(thisClassType));
    javaMethods.add(createIsParseableFromMethod(templateFinalVarExprs));

    return javaMethods;
  }

  private static MethodDefinition createParseMethod(
      TypeNode thisClassType,
      List<VariableExpr> templateFinalVarExprs,
      List<List<String>> tokenHierarchies,
      Map<String, TypeNode> types) {
    String formattedStringArgName = "formattedString";
    VariableExpr formattedStringArgExpr =
        VariableExpr.withVariable(
            Variable.builder().setName(formattedStringArgName).setType(TypeNode.STRING).build());
    String exceptionMessageString =
        String.format(
            "%s.parse: %s not in valid format",
            thisClassType.reference().name(), formattedStringArgName);

    ValueExpr exceptionMessageExpr =
        ValueExpr.withValue(StringObjectValue.withValue(exceptionMessageString));
    TypeNode mapStringType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(Map.class)
                .setGenerics(
                    Arrays.asList(
                        ConcreteReference.withClazz(String.class),
                        ConcreteReference.withClazz(String.class)))
                .build());
    VariableExpr matchMapVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("matchMap").setType(mapStringType).build());

    List<Statement> body = new ArrayList<>();
    body.add(
        IfStatement.builder()
            .setConditionExpr(
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(formattedStringArgExpr)
                    .setMethodName("isEmpty")
                    .setReturnType(TypeNode.BOOLEAN)
                    .build())
            .setBody(
                Arrays.asList(
                    ExprStatement.withExpr(
                        ReturnExpr.withExpr(ValueExpr.withValue(NullObjectValue.create())))))
            .build());

    List<Expr> formattedStringArgList = Arrays.asList(formattedStringArgExpr);
    List<VariableExpr> formattedStringArgDeclList =
        Arrays.asList(formattedStringArgExpr.toBuilder().setIsDecl(true).build());
    boolean hasVariants = tokenHierarchies.size() > 1;
    if (!hasVariants) {
      List<Expr> methodArgs = Arrays.asList(formattedStringArgExpr, exceptionMessageExpr);
      MethodInvocationExpr validatedMatchExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(templateFinalVarExprs.get(0))
              .setMethodName("validatedMatch")
              .setArguments(methodArgs)
              .setReturnType(mapStringType)
              .build();

      AssignmentExpr matchMapAssignExpr =
          AssignmentExpr.builder()
              .setVariableExpr(matchMapVarExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(validatedMatchExpr)
              .build();
      body.add(ExprStatement.withExpr(matchMapAssignExpr));

      List<Expr> ofMethodArgExprs =
          tokenHierarchies.get(0).stream()
              .map(
                  t ->
                      MethodInvocationExpr.builder()
                          .setExprReferenceExpr(matchMapVarExpr)
                          .setMethodName("get")
                          .setArguments(
                              Arrays.asList(ValueExpr.withValue(StringObjectValue.withValue(t))))
                          .build())
              .collect(Collectors.toList());

      MethodInvocationExpr ofMethodExpr =
          MethodInvocationExpr.builder()
              .setMethodName("of")
              .setArguments(ofMethodArgExprs)
              .setReturnType(thisClassType)
              .build();
      return MethodDefinition.builder()
          .setScope(ScopeNode.PUBLIC)
          .setIsStatic(true)
          .setReturnType(thisClassType)
          .setName("parse")
          .setArguments(formattedStringArgDeclList)
          .setBody(body)
          .setReturnExpr(ofMethodExpr)
          .build();
    }

    IfStatement.Builder ifStatementBuilder = IfStatement.builder();
    String ofMethodNamePattern = "of%s";
    for (int i = 0; i < tokenHierarchies.size(); i++) {
      VariableExpr templateVarExpr = templateFinalVarExprs.get(i);
      MethodInvocationExpr conditionExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(templateVarExpr)
              .setMethodName("matches")
              .setArguments(formattedStringArgList)
              .setReturnType(TypeNode.BOOLEAN)
              .build();

      MethodInvocationExpr matchValueExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(templateVarExpr)
              .setMethodName("match")
              .setArguments(formattedStringArgList)
              .setReturnType(mapStringType)
              .build();
      AssignmentExpr matchMapAssignExpr =
          AssignmentExpr.builder()
              .setVariableExpr(matchMapVarExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(matchValueExpr)
              .build();

      List<String> tokens = tokenHierarchies.get(i);
      MethodInvocationExpr ofMethodExpr =
          MethodInvocationExpr.builder()
              .setMethodName(String.format(ofMethodNamePattern, getBuilderTypeName(tokens)))
              .setArguments(
                  tokens.stream()
                      .map(
                          t ->
                              MethodInvocationExpr.builder()
                                  .setExprReferenceExpr(matchMapVarExpr)
                                  .setMethodName("get")
                                  .setArguments(
                                      Arrays.asList(
                                          ValueExpr.withValue(StringObjectValue.withValue(t))))
                                  .build())
                      .collect(Collectors.toList()))
              .setReturnType(thisClassType)
              .build();

      ReturnExpr subReturnExpr = ReturnExpr.withExpr(ofMethodExpr);

      List<Statement> ifStatements =
          Arrays.asList(matchMapAssignExpr, subReturnExpr).stream()
              .map(e -> ExprStatement.withExpr(e))
              .collect(Collectors.toList());
      if (i == 0) {
        ifStatementBuilder =
            ifStatementBuilder.setConditionExpr(conditionExpr).setBody(ifStatements);
      } else {
        ifStatementBuilder = ifStatementBuilder.addElseIf(conditionExpr, ifStatements);
      }
    }

    body.add(ifStatementBuilder.build());
    body.add(
        ExprStatement.withExpr(
            ThrowExpr.builder()
                .setType(STATIC_TYPES.get("ValidationException"))
                .setMessage(exceptionMessageString)
                .build()));
    return MethodDefinition.builder()
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setReturnType(thisClassType)
        .setName("parse")
        .setArguments(formattedStringArgDeclList)
        .setBody(body)
        .build();
  }

  private static MethodDefinition createParseListMethod(TypeNode thisClassType) {
    TypeNode listStringType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(List.class)
                .setGenerics(Arrays.asList(ConcreteReference.withClazz(String.class)))
                .build());
    TypeNode returnType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(List.class)
                .setGenerics(Arrays.asList(thisClassType.reference()))
                .build());

    VariableExpr formattedStringsVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("formattedStrings").setType(listStringType).build());
    VariableExpr listVarExpr =
        VariableExpr.withVariable(Variable.builder().setName("list").setType(returnType).build());

    AssignmentExpr listAssignExpr =
        AssignmentExpr.builder()
            .setVariableExpr(listVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(
                NewObjectExpr.builder()
                    .setType(
                        TypeNode.withReference(
                            ConcreteReference.builder().setClazz(ArrayList.class).build()))
                    .setIsGeneric(true)
                    .setArguments(
                        Arrays.asList(
                            MethodInvocationExpr.builder()
                                .setExprReferenceExpr(formattedStringsVarExpr)
                                .setMethodName("size")
                                .build()))
                    .build())
            .build();

    VariableExpr singleStrVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("formattedString").setType(TypeNode.STRING).build());
    ForStatement forStatement =
        ForStatement.builder()
            .setLocalVariableExpr(singleStrVarExpr.toBuilder().setIsDecl(true).build())
            .setCollectionExpr(formattedStringsVarExpr)
            .setBody(
                Arrays.asList(
                    ExprStatement.withExpr(
                        MethodInvocationExpr.builder()
                            .setExprReferenceExpr(listVarExpr)
                            .setMethodName("add")
                            .setArguments(
                                Arrays.asList(
                                    MethodInvocationExpr.builder()
                                        .setMethodName("parse")
                                        .setArguments(Arrays.asList(singleStrVarExpr))
                                        .build()))
                            .build())))
            .build();

    return MethodDefinition.builder()
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setReturnType(returnType)
        .setName("parseList")
        .setArguments(Arrays.asList(formattedStringsVarExpr.toBuilder().setIsDecl(true).build()))
        .setBody(Arrays.asList(ExprStatement.withExpr(listAssignExpr), forStatement))
        .setReturnExpr(listVarExpr)
        .build();
  }

  private static MethodDefinition createToStringListMethod(TypeNode thisClassType) {
    TypeNode listClassType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(List.class)
                .setGenerics(Arrays.asList(thisClassType.reference()))
                .build());
    VariableExpr valuesVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("values").setType(listClassType).build());

    TypeNode listStringType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(List.class)
                .setGenerics(Arrays.asList(ConcreteReference.withClazz(String.class)))
                .build());
    VariableExpr listVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("list").setType(listStringType).build());

    AssignmentExpr listAssignExpr =
        AssignmentExpr.builder()
            .setVariableExpr(listVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(
                NewObjectExpr.builder()
                    .setType(TypeNode.withReference(ConcreteReference.withClazz(ArrayList.class)))
                    .setIsGeneric(true)
                    .setArguments(
                        Arrays.asList(
                            MethodInvocationExpr.builder()
                                .setExprReferenceExpr(valuesVarExpr)
                                .setMethodName("size")
                                .build()))
                    .build())
            .build();

    // TODO(miraleung): Use equality check instead of Objects.
    Expr isNullCheck =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(STATIC_TYPES.get("Objects"))
            .setMethodName("equals")
            .setArguments(
                Arrays.asList(valuesVarExpr, ValueExpr.withValue(NullObjectValue.create())))
            .setReturnType(TypeNode.BOOLEAN)
            .build();
    Statement listAddEmptyStringStatement =
        ExprStatement.withExpr(
            MethodInvocationExpr.builder()
                .setExprReferenceExpr(listVarExpr)
                .setMethodName("add")
                .setArguments(Arrays.asList(ValueExpr.withValue(StringObjectValue.withValue(""))))
                .build());

    VariableExpr valueVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("value").setType(thisClassType).build());
    Statement listAddValueStatement =
        ExprStatement.withExpr(
            MethodInvocationExpr.builder()
                .setExprReferenceExpr(listVarExpr)
                .setMethodName("add")
                .setArguments(
                    Arrays.asList(
                        MethodInvocationExpr.builder()
                            .setExprReferenceExpr(valueVarExpr)
                            .setMethodName("toString")
                            .build()))
                .build());

    IfStatement ifStatement =
        IfStatement.builder()
            .setConditionExpr(isNullCheck)
            .setBody(Arrays.asList(listAddEmptyStringStatement))
            .setElseBody(Arrays.asList(listAddValueStatement))
            .build();
    ForStatement forStatement =
        ForStatement.builder()
            .setLocalVariableExpr(valueVarExpr.toBuilder().setIsDecl(true).build())
            .setCollectionExpr(valuesVarExpr)
            .setBody(Arrays.asList(ifStatement))
            .build();

    return MethodDefinition.builder()
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setReturnType(listStringType)
        .setName("toStringList")
        .setArguments(Arrays.asList(valuesVarExpr.toBuilder().setIsDecl(true).build()))
        .setBody(Arrays.asList(ExprStatement.withExpr(listAssignExpr), forStatement))
        .setReturnExpr(listVarExpr)
        .build();
  }

  private static MethodDefinition createIsParseableFromMethod(
      List<VariableExpr> templateFinalVarExprs) {
    VariableExpr formattedStringVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("formattedString").setType(TypeNode.STRING).build());
    MethodInvocationExpr returnOrExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(templateFinalVarExprs.get(0))
            .setMethodName("matches")
            .setArguments(Arrays.asList(formattedStringVarExpr))
            .build();
    for (int i = 1; i < templateFinalVarExprs.size(); i++) {
      // TODO(miraleung): Use actual or operations here.
      returnOrExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(returnOrExpr)
              .setMethodName("todoOr")
              .setArguments(
                  Arrays.asList(
                      MethodInvocationExpr.builder()
                          .setExprReferenceExpr(templateFinalVarExprs.get(i))
                          .setMethodName("matches")
                          .setArguments(Arrays.asList(formattedStringVarExpr))
                          .build()))
              .setReturnType(TypeNode.BOOLEAN)
              .build();
    }

    return MethodDefinition.builder()
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setReturnType(TypeNode.BOOLEAN)
        .setName("isParsableFrom")
        .setArguments(Arrays.asList(formattedStringVarExpr.toBuilder().setIsDecl(true).build()))
        .setReturnExpr(returnOrExpr)
        .build();
  }

  private static Map<String, TypeNode> createStaticTypes() {

    List<Class> concreteClazzes =
        Arrays.asList(
            ArrayList.class,
            BetaApi.class,
            Generated.class,
            ImmutableMap.class,
            List.class,
            Map.class,
            Objects.class,
            PathTemplate.class,
            Preconditions.class,
            com.google.api.resourcenames.ResourceName.class,
            ValidationException.class);
    return concreteClazzes.stream()
        .collect(
            Collectors.toMap(
                c -> c.getSimpleName(),
                c -> TypeNode.withReference(ConcreteReference.withClazz(c))));
  }

  private static Map<String, TypeNode> createDynamicTypes(
      ResourceName resourceName, List<List<String>> tokenHierarchies) {
    String thisClassName = getThisClassName(resourceName);
    Map<String, TypeNode> dynamicTypes = new HashMap<>();
    dynamicTypes.put(
        thisClassName,
        TypeNode.withReference(
            VaporReference.builder()
                .setName(thisClassName)
                .setPakkage(resourceName.pakkage())
                .build()));
    dynamicTypes.put(
        "Builder",
        TypeNode.withReference(
            VaporReference.builder()
                .setName("Builder")
                .setPakkage(resourceName.pakkage())
                .setEnclosingClassName(thisClassName)
                .setIsStaticImport(true)
                .build()));

    if (tokenHierarchies.size() > 1) {
      dynamicTypes.putAll(
          tokenHierarchies.subList(1, tokenHierarchies.size()).stream()
              .map(ts -> getBuilderTypeName(ts))
              .collect(
                  Collectors.toMap(
                      s -> s,
                      s ->
                          TypeNode.withReference(
                              VaporReference.builder()
                                  .setName(s)
                                  .setPakkage(resourceName.pakkage())
                                  .setEnclosingClassName(thisClassName)
                                  .setIsStaticImport(true)
                                  .build()))));
    }
    return dynamicTypes;
  }

  private static Map<String, VariableExpr> createFixedClassMemberVariables() {
    Map<String, TypeNode> memberVars = new HashMap<>();
    Reference stringRef = ConcreteReference.withClazz(String.class);
    memberVars.put(
        "fieldValuesMap",
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(Map.class)
                .setGenerics(Arrays.asList(stringRef, stringRef))
                .build()));
    memberVars.put(
        "pathTemplate", TypeNode.withReference(ConcreteReference.withClazz(PathTemplate.class)));
    memberVars.put("fixedValue", TypeNode.STRING);
    return memberVars.entrySet().stream()
        .map(e -> Variable.builder().setName(e.getKey()).setType(e.getValue()).build())
        .collect(Collectors.toMap(v -> v.identifier().name(), v -> VariableExpr.withVariable(v)));
  }

  private static String getThisClassName(ResourceName resourceName) {
    return String.format(
        CLASS_NAME_PATTERN, JavaStyle.toUpperCamelCase(resourceName.resourceTypeName()));
  }

  private static String getBuilderTypeName(List<List<String>> tokenHierarchies, int index) {
    return index == 0 ? "Builder" : getBuilderTypeName(tokenHierarchies.get(index));
  }

  private static String getBuilderTypeName(List<String> tokens) {
    return String.format("%sBuilder", concatToUpperCamelCaseName(tokens));
  }

  private static TypeNode getBuilderType(
      Map<String, TypeNode> types, List<List<String>> tokenHierarchies, int index) {
    return index == 0
        ? types.get("Builder")
        : types.get(getBuilderTypeName(tokenHierarchies, index));
  }

  @VisibleForTesting
  static List<List<String>> parseTokenHierarchy(List<String> patterns) {
    List<List<String>> tokenHierachies = new ArrayList<>();
    // Assumes that resource definitions do not have non-slash separators.
    for (String pattern : patterns) {
      List<String> hierarchy = new ArrayList<>();
      Set<String> vars = PathTemplate.create(pattern).vars();
      String[] patternTokens = pattern.split(SLASH);
      for (String patternToken : patternTokens) {
        if (patternToken.startsWith(LEFT_BRACE) && patternToken.endsWith(RIGHT_BRACE)) {
          hierarchy.add(
              vars.stream()
                  .filter(v -> patternToken.contains(v))
                  .collect(Collectors.toList())
                  .get(0));
        }
      }
      tokenHierachies.add(hierarchy);
    }
    return tokenHierachies;
  }

  @VisibleForTesting
  static Set<String> getTokenSet(List<List<String>> tokenHierarchy) {
    return tokenHierarchy.stream()
        .flatMap(tokens -> tokens.stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @VisibleForTesting
  static String concatToUpperSnakeCaseName(List<String> tokens) {
    // Tokens are currently in lower_snake_case space.
    return JavaStyle.toUpperSnakeCase(tokens.stream().collect(Collectors.joining("_")));
  }

  @VisibleForTesting
  static String concatToUpperCamelCaseName(List<String> tokens) {
    // Tokens are currently in lower_snake_case space.
    return JavaStyle.toUpperCamelCase(tokens.stream().collect(Collectors.joining("_")));
  }
}
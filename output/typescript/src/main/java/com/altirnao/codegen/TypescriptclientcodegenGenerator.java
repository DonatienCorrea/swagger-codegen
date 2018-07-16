package com.altirnao.codegen;

import io.swagger.codegen.*;
import io.swagger.models.properties.*;
import io.swagger.codegen.languages.AbstractTypeScriptClientCodegen;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class TypescriptclientcodegenGenerator extends AbstractTypeScriptClientCodegen implements CodegenConfig {
  
  private static final String X_DISCRIMINATOR_TYPE = "x-discriminator-value";

  // source folder where to write the files
  protected String sourceFolder = "src";
  protected String apiVersion = "1.0.0";

  /**
   * Configures the type of generator.
   * 
   * @return  the CodegenType for this generator
   * @see     io.swagger.codegen.CodegenType
   */
  public CodegenType getTag() {
    return CodegenType.CLIENT;
  }

  /**
   * Configures a friendly name for the generator.  This will be used by the generator
   * to select the library with the -l flag.
   * 
   * @return the friendly name for the generator
   */
  public String getName() {
    return "TypescriptClientCodegen";
  }

  /**
   * Returns human-friendly help for the generator.  Provide the consumer with help
   * tips, parameters here
   * 
   * @return A string value for the help message
   */
  public String getHelp() {
    return "Generates a TypescriptClientCodegen client library.";
  }

  public TypescriptclientcodegenGenerator() {
    super();

    // set the output folder here
    outputFolder = "output/typescriptClient";

    /**
     * Models.  You can write model files using the modelTemplateFiles map.
     * if you want to create one template for file, you can do so here.
     * for multiple files for model, just put another entry in the `modelTemplateFiles` with
     * a different extension
     */
    modelTemplateFiles.put(
      "resource.mustache", // the template to use
      ".d.ts");       // the extension for each file to write

    /**
     * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
     * as with models, add multiple entries with different extensions for multiple files per
     * class
     */
    apiTemplateFiles.put(
      "api.mustache",   // the template to use
      ".d.ts");       // the extension for each file to write
    
    // Parameters file per api class. 
    apiTemplateFiles.put("parameters.mustache", "Parameters.d.ts");

    /**
     * Template Location.  This is the location which templates will be read from.  The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "TypescriptClientCodegen";

    /**
     * Api Package.  Optional, if needed, this can be used in templates
     */
    apiPackage = "api";

    /**
     * Model Package.  Optional, if needed, this can be used in templates
     */
    modelPackage = "Resources";

    /**
     * Additional Properties.  These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */
    additionalProperties.put("apiVersion", apiVersion);

    /**
     * Supporting Files.  You can write single files for the generator with the
     * entire object tree available.  If the input file has a suffix of `.mustache
     * it will be processed by the template engine.  Otherwise, it will be copied
     */
    // [template filename], [relative path], [output file name]
    supportingFiles.add(new SupportingFile("resources.mustache", "src/Resources", "resources.d.ts"));

    /**
     * Language Specific Primitives.  These types will not trigger imports by
     * the client generator
     
    languageSpecificPrimitives = new HashSet<String>(
      Arrays.asList(
        "Type1",      // replace these with your types
        "Type2")
    );*/
  }

  /**
   * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
   * those terms here.  This logic is only called if a variable matches the reserved words
   * 
   * @return the escaped term
   */
  @Override
  public String escapeReservedWord(String name) {
    return "_" + name;  // add an underscore to the name
  }

  /**
   * Location to write model files.  You can use the modelPackage() as defined when the class is
   * instantiated
   */
  public String modelFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
  }

  /**
   * Location to write api files.  You can use the apiPackage() as defined when the class is
   * instantiated
   */
  @Override
  public String apiFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
  }

  /**
   * Optional - type declaration.  This is a String which is used by the templates to instantiate your
   * types.  There is typically special handling for different property types
   *
   * @return a string value used as the `dataType` field for model templates, `returnType` for api templates
   */
  @Override
  public String getTypeDeclaration(Property p) {
    if(p instanceof ArrayProperty) {
      ArrayProperty ap = (ArrayProperty) p;
      Property inner = ap.getItems();
      return getSwaggerType(p) + "[" + getTypeDeclaration(inner) + "]";
    }
    else if (p instanceof MapProperty) {
      MapProperty mp = (MapProperty) p;
      Property inner = mp.getAdditionalProperties();
      return getSwaggerType(p) + "[String, " + getTypeDeclaration(inner) + "]";
    }
    return super.getTypeDeclaration(p);
  }


  // adding methods from TypeScriptAngularJsClientCodegen.java

  /**
   * Optional - swagger type conversion.  This is used to map swagger types in a `Property` into 
   * either language specific types via `typeMapping` or into complex models if there is not a mapping.
   *
   * @return a string value of the type or complex model for this property
   * @see io.swagger.models.properties.Property
   */
  @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        if(isLanguagePrimitive(swaggerType) || isLanguageGenericType(swaggerType)) {
            return swaggerType;
        }
        return addModelPrefix(swaggerType);
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) {
        super.postProcessParameter(parameter);
        parameter.dataType = addModelPrefix(parameter.dataType);
    }

    @Override
    public Map<String, Object> postProcessAllModels(Map<String, Object> objs) {
        Map<String, Object> result = super.postProcessAllModels(objs);

        for (Map.Entry<String, Object> entry : result.entrySet()) {
            Map<String, Object> inner = (Map<String, Object>) entry.getValue();
            List<Map<String, Object>> models = (List<Map<String, Object>>) inner.get("models");
            for (Map<String, Object> mo : models) {
                CodegenModel cm = (CodegenModel) mo.get("model");
                if (cm.discriminator != null && cm.children != null) {
                    for (CodegenModel child : cm.children) {
                        this.setDiscriminatorValue(child, cm.discriminator, this.getDiscriminatorValue(child));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String toOperationId(String operationId) {
      // throw exception if method name is empty
      if (StringUtils.isEmpty(operationId)) {
          throw new RuntimeException("Empty method name (operationId) not allowed");
      }

      // method name cannot use reserved keyword, e.g. return
      // append _ at the beginning, e.g. _return
      if (isReservedWord(operationId)) {
          return escapeReservedWord(operationId.substring(operationId.lastIndexOf('.')));
      }

      return operationId.substring(operationId.lastIndexOf('.') + 1);
  }

    private void setDiscriminatorValue(CodegenModel model, String baseName, String value) {
        for (CodegenProperty prop : model.allVars) {
            if (prop.baseName.equals(baseName)) {
                prop.discriminatorValue = value;
            }
        }
        if (model.children != null) {
            final boolean newDiscriminator = model.discriminator != null;
            for (CodegenModel child : model.children) {
                this.setDiscriminatorValue(child, baseName, newDiscriminator ? value : this.getDiscriminatorValue(child));
            }
        }
    }

    private String getDiscriminatorValue(CodegenModel model) {
        return model.vendorExtensions.containsKey(X_DISCRIMINATOR_TYPE) ?
            (String) model.vendorExtensions.get(X_DISCRIMINATOR_TYPE) : model.classname;
    }

    private String getIndexDirectory() {
        String indexPackage = modelPackage.substring(0, Math.max(0, modelPackage.lastIndexOf('.')));
        return indexPackage.replace('.', File.separatorChar);
    }

    private String addModelPrefix(String swaggerType) {
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
        } else {
            type = swaggerType;
        }

        if (!isLanguagePrimitive(type) && !isLanguageGenericType(type)) {
            type = "Resources." + swaggerType;
        }
        return type;
    }

    private boolean isLanguagePrimitive(String type) {
        return languageSpecificPrimitives.contains(type);
    }

    private boolean isLanguageGenericType(String type) {
        for (String genericType: languageGenericTypes) {
            if (type.startsWith(genericType + "<"))  {
                return true;
            }
        }
        return false;
    }
}
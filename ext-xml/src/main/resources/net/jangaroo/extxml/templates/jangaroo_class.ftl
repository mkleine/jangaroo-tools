<#-- @ftlvariable name="" type="net.jangaroo.extxml.ComponentClass" -->
package ${packageName} {

import ext.ComponentMgr;
<#list imports as import>
import ${import};
</#list>

public class ${className} extends ${superClassName} {

  public const xtype:String = "${xtype}";
{
  ext.ComponentMgr.registerType(xtype, ${className});
}

  public function ${className}(config:* = undefined) {
    super(Ext.apply(config, ${json}));
  }

}
}
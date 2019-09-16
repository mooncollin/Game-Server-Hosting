package frontend;

import java.lang.reflect.InvocationTargetException;

import forms.Input;
import html.CompoundElement;

public class BootstrapTemplates
{
	public static CompoundElement settingsInput(Class<? extends Input> inputType, String labelText, String id, String placeholder, String value, String smallText, boolean required, boolean disabled)
	{
		CompoundElement listItem = new CompoundElement("li");
		listItem.addClasses("list-group-item", "form-group", "form-inline");
		CompoundElement label = new CompoundElement("label", labelText);
		label.setAttribute("for", id);
		label.addClasses("d-inline-block", "w-25", "align-middle");
		Input field;
		try
		{
			field = inputType.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e)
		{
			return null;
		}
		
		field.setName(id);
		field.setID(id);
		field.addClass("form-control");
		if(placeholder != null)
		{
			field.setAttribute("placeholder", placeholder);
		}
		
		
		if(inputType.equals(forms.Checkbox.class))
		{
			field.setDisabled(disabled);
			((forms.Checkbox) field).setChecked(value.equals("true"));
		}
		else
		{
			field.setReadOnly(disabled);
			field.setValue(value);
		}
		field.setRequired(required);
		
		listItem.addElement(label);
		listItem.addElement(field);

		if(smallText != null)
		{
			CompoundElement small = new CompoundElement("small", smallText);
			small.addClasses("form-text", "text-muted");
			listItem.addElement(small);
		}
		
		return listItem;
	}
}

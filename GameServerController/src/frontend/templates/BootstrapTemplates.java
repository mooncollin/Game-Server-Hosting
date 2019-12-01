package frontend.templates;

import java.lang.reflect.InvocationTargetException;

import attributes.Attributes;
import forms.Input;
import tags.LI;
import tags.Label;
import tags.Small;
import tags.Span;

public class BootstrapTemplates
{
	@SuppressWarnings("unchecked")
	public static LI settingsInput(Class<? extends Input> inputType, String labelText, String id, String placeholder, String value, String smallText, boolean required, boolean disabled)
	{
		try
		{
			return new LI()
					.addClasses("list-group-item", "form-group", "form-inline")
					.addElements
					(
						new Label(labelText, Attributes.makeAttribute("for", id))
							.addClasses("d-inline-block", "w-25", "align-middle"),
						inputType.getConstructor().newInstance()
						.addClasses("form-control")
						.addAttributes
						(
							Attributes.Name.makeAttribute(id),
							Attributes.ID.makeAttribute(id),
							Attributes.Required.makeAttribute(required),
							placeholder != null ? Attributes.PlaceHolder.makeAttribute(placeholder) : null,
							inputType.equals(forms.Checkbox.class) ? Attributes.Disabled.makeAttribute(disabled) : Attributes.ReadOnly.makeAttribute(disabled),
							inputType.equals(forms.Checkbox.class) ? Attributes.Checked.makeAttribute(value.equals("true")) : Attributes.Value.makeAttribute(value)
						),
						smallText != null ? new Small(smallText).addClasses("form-text", "text-muted") : null
					);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e)
		{
			throw new RuntimeException(e.getMessage());
		}
	}
	
	public static Span makeSpinner(String type, boolean small)
	{
		return new Span().addClasses("spinner-border", type != null ? "text-" + type : null, small ? "spinner-border-sm" : null);
	}
}

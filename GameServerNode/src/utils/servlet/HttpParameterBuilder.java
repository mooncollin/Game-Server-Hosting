package utils.servlet;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class HttpParameterBuilder<T>
{
	private Class<T> clazz;
	private String name;
	private boolean nullable;
	private Predicate<T> checker;
	private Function<String, Optional<T>> parser;
	
	private HttpParameterBuilder(Class<T> clazz)
	{
		this.clazz = clazz;
	}
	
	public static <F> HttpParameterBuilder<F> start(Class<F> clazz)
	{
		return new HttpParameterBuilder<F>(clazz);
	}
	
	public HttpParameterBuilder<T> setName(String name)
	{
		this.name = Objects.requireNonNull(name);
		return this;
	}
	
	public HttpParameterBuilder<T> isNullable(boolean n)
	{
		this.nullable = n;
		return this;
	}
	
	public HttpParameterBuilder<T> setChecker(Predicate<T> checker)
	{
		this.checker = checker;
		return this;
	}
	
	public HttpParameterBuilder<T> setParser(Function<String, Optional<T>> parser)
	{
		this.parser = parser;
		return this;
	}
	
	public HttpParameter<T> build()
	{
		return new HttpParameter<T>(clazz, Objects.requireNonNull(name), nullable, checker, parser);
	}
}

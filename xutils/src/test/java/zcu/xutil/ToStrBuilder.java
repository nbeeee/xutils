package zcu.xutil;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.Arrays;


public class ToStrBuilder {

	private final StringBuilder builder;
	  public ToStrBuilder(Class<?> type) {
		  this(type.getSimpleName());
	  }
	  public ToStrBuilder(String name) {
	    this.builder = new StringBuilder(name).append('[');
	  }

	  public ToStrBuilder append(String name, Object value) {
		builder.append(name).append('=').append(value).append(',');
	    return this;
	  }
	  public ToStrBuilder append(String name, Object[] values) {
			builder.append(name).append('=').append(Arrays.toString(values)).append(',');
		    return this;
		 }
	  public ToStrBuilder append(String name, byte[] bytes) {
			builder.append(name).append('=').append(Arrays.toString(bytes)).append(',');
		    return this;
		 }
	  public ToStrBuilder append(String name, char[] chars) {
			builder.append(name).append('=').append(Arrays.toString(chars)).append(',');
		    return this;
		 }
	  public ToStrBuilder append(String name, short[] shorts) {
			builder.append(name).append('=').append(Arrays.toString(shorts)).append(',');
		    return this;
		 }
	  public ToStrBuilder append(String name, int[] ints) {
			builder.append(name).append('=').append(Arrays.toString(ints)).append(',');
		    return this;
		 }
	  public ToStrBuilder append(String name, long[] longs) {
			builder.append(name).append('=').append(Arrays.toString(longs)).append(',');
		    return this;
		 }
	  public ToStrBuilder append(String name, double[] doubles) {
			builder.append(name).append('=').append(Arrays.toString(doubles)).append(',');
			return this;
		 }
	  public ToStrBuilder append(String name, float[] floats) {
			builder.append(name).append('=').append(Arrays.toString(floats)).append(',');
		    return this;
		 }
	  public ToStrBuilder append(String name, boolean[] bools) {
			builder.append(name).append('=').append(Arrays.toString(bools)).append(',');
		    return this;
		 }
	@Override
	public String toString() {
		int last = builder.length()-1;
		char lastChar=builder.charAt(last);
		if(lastChar==',')
			builder.setCharAt(last, ']');
		else if(lastChar=='[')
			builder.append(']');
	    return builder.toString();
	  }
}

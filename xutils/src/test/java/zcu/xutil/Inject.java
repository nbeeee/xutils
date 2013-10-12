package zcu.xutil;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

@Target({CONSTRUCTOR, FIELD })
@Retention(RUNTIME)
public @interface Inject {
  String value();
}

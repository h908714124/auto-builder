package net.autobuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marker annotation for
 * <a href="https://github.com/h908714124/auto-builder">auto-builder</a>.</p>
 *
 * <p>This annotation specifies a custom post build method, which must be a non-abstract, non-private
 * instance method of an {@link net.autobuilder.AutoBuilder AutoBuilder} annotated class</p>
 *
 * <p>The annotated method must may not have any arguments.
 * It may return anything. For example, it may simply return {@code this}.
 * It may also perform validation by throwing an exception.
 * This exception can (or should) be handled when the generated builder's {@code build()} method is invoked.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface PostBuild {
}

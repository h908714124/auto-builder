package net.autobuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marker annotation for
 * <a href="https://github.com/h908714124/auto-builder">auto-builder</a>.</p>
 *
 * <ul>
 * <li>This won't do anything,
 * unless the annotated class also has an {@code @AutoValue} annotation.
 * See <a href="https://github.com/google/auto/tree/master/value">here</a> for
 * details.</li>
 * <li>The annotated class must be a &quot;regular&quot; auto-value class,
 * not the &quot;builder&quot; variety.</li>
 * <li>The annotated class may not have any type parameters.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoBuilder {

  /**
   * <p>If true, the generated code will attempt to reuse
   * builder instances. This will improve performance, unless builder calls are
   * stacked, i.e. a new builder instance is needed before {@code build()}
   * has been called on the &quot;previous&quot; builder instance.</p>
   *
   * <p><em>Note:</em>The generated code will store the builder instance in a static
   * {@link ThreadLocal} field.
   * Leave this at {@code false} if you wish to prevent this.</p>
   *
   * @return whether the generated code should cache and reuse builder instances
   */
  boolean reuseBuilder() default false;
}

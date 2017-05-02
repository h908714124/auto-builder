package net.autobuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for
 * <a href="https://github.com/h908714124/auto-builder">AutoBuilder</a>.
 * <ul>
 * <li>This won't do anything,
 * unless the class also has an @AutoValue annotation.</li>
 * <li>The annotated class should be a "regular" auto-value class,
 * not the "builder" variety.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoBuilder {
}

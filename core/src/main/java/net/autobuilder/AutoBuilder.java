package net.autobuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for
 * <a href="https://github.com/h908714124/auto-builder">auto-builder</a>.
 * <ul>
 * <li>This won't do anything,
 * unless the class also has an @AutoValue annotation.</li>
 * <li>The annotated class should be a "regular" auto-value class,
 * not the "builder" variety.</li>
 * <li>I'm not sure how this combines with auto-value <i>extensions</i>.
 * If you find an annoying behaviour,
 * please report it at the above mentioned github page.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoBuilder {
}

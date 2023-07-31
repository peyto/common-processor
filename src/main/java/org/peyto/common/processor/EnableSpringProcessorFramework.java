package org.peyto.common.processor;

import org.peyto.common.processor.core.ProcessorFrameworkConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({ProcessorFrameworkConfiguration.class})
public @interface EnableSpringProcessorFramework {

}

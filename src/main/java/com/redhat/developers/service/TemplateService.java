package com.redhat.developers.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

@Service
@Slf4j
public class TemplateService {

    @Autowired
    Configuration freeMarkerConfiguration;

    /**
     * @param context
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    public String buildFactoryJsonFromTemplate(Map<String, String> context) throws IOException, TemplateException {

        Template template = freeMarkerConfiguration.getTemplate(".factory.json.ftl");

        Writer writer = new StringWriter();
        template.process(context, writer);
        writer.flush();
        String factoryJson = writer.toString();
        writer.close();
        return factoryJson;
    }
}

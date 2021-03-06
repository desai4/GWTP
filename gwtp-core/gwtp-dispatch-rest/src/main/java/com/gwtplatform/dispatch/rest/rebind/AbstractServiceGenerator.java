/**
 * Copyright 2014 ArcBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gwtplatform.dispatch.rest.rebind;

import java.util.List;

import javax.inject.Provider;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.google.common.collect.Lists;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.gwtplatform.dispatch.rest.rebind.type.ActionBinding;
import com.gwtplatform.dispatch.rest.rebind.type.ServiceBinding;
import com.gwtplatform.dispatch.rest.rebind.type.ServiceDefinitions;
import com.gwtplatform.dispatch.rest.rebind.util.GeneratorUtil;

public abstract class AbstractServiceGenerator extends AbstractVelocityGenerator {
    protected static final String TEMPLATE = "com/gwtplatform/dispatch/rest/rebind/RestService.vm";

    private final List<ActionBinding> actionBindings = Lists.newArrayList();
    private final List<ServiceBinding> serviceBindings = Lists.newArrayList();
    private final ServiceDefinitions serviceDefinitions;
    private final GeneratorFactory generatorFactory;
    private final JClassType service;

    protected AbstractServiceGenerator(
            TypeOracle typeOracle,
            Logger logger,
            Provider<VelocityContext> velocityContextProvider,
            VelocityEngine velocityEngine,
            GeneratorUtil generatorUtil,
            ServiceDefinitions serviceDefinitions,
            GeneratorFactory generatorFactory,
            JClassType service) {
        super(typeOracle, logger, velocityContextProvider, velocityEngine, generatorUtil);

        this.serviceDefinitions = serviceDefinitions;
        this.generatorFactory = generatorFactory;
        this.service = service;
    }

    @Override
    protected String getPackage() {
        return service.getPackage().getName().replace(SHARED_PACKAGE, CLIENT_PACKAGE);
    }

    protected abstract ServiceBinding getServiceBinding();

    @Override
    protected void populateVelocityContext(VelocityContext velocityContext) throws UnableToCompleteException {
        velocityContext.put("serviceInterface", service);
        velocityContext.put("actionBindings", actionBindings);
        velocityContext.put("serviceBindings", serviceBindings);
    }

    protected void generateMethods() throws UnableToCompleteException {
        JMethod[] methods = service.getInheritableMethods();
        if (methods != null) {
            for (JMethod method : methods) {
                if (isRestService(method)) {
                    generateChildRestService(method);
                } else {
                    generateRestAction(method);
                }
            }
        }
    }

    protected boolean isRestService(JMethod method) throws UnableToCompleteException {
        JClassType returnInterface = method.getReturnType().isInterface();

        return returnInterface != null && serviceDefinitions.isService(returnInterface);
    }

    protected void generateChildRestService(JMethod method) throws UnableToCompleteException {
        ChildServiceGenerator generator = generatorFactory.createChildServiceGenerator(method, getServiceBinding());
        serviceBindings.add(generator.generate());
    }

    protected void generateRestAction(JMethod method) throws UnableToCompleteException {
        ActionGenerator generator = generatorFactory.createActionGenerator(method, getServiceBinding());
        actionBindings.add(generator.generate());
    }
}

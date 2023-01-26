package de.seepex.controller;

import de.seepex.config.RpcResourceProvider;
import de.seepex.config.RpcResourceSupplier;
import de.seepex.domain.*;
import de.seepex.service.ServiceCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class DocController {

    @Autowired
    private ServiceCollector serviceCollector;

    @Autowired
    private RpcResourceSupplier rpcResourceSupplier;

    @RequestMapping(value = RpcResourceProvider.SPX_RPC_JSONDOC_PATH, method = RequestMethod.GET)
    public ResponseEntity<List<JsonSpxClass>> jsonDoc() {

        List<JsonSpxClass> result = new ArrayList<>();
        List<SpxClass> classes = serviceCollector.getClasses();

        for(SpxClass spxClass: classes) {
            List<Method> methods = serviceCollector.getMethods(spxClass.getId());

            JsonSpxClass jsonSpxClass = new JsonSpxClass();
            jsonSpxClass.setId(spxClass.getId());
            jsonSpxClass.setName(spxClass.getName());
            jsonSpxClass.setDescription(spxClass.getDescription());

            List<JsonMethod> jsonMethods = new ArrayList<>();

            for(Method method : methods) {
                List<JsonParameter> parameters = new ArrayList<>();
                for(Parameter parameter : method.getParameters()) {
                    JsonParameter jsonParameter = new JsonParameter();
                    jsonParameter.setName(parameter.getName());
                    jsonParameter.setType(parameter.getType().getName());

                    parameters.add(jsonParameter);
                }

                JsonMethod jsonMethod = new JsonMethod();
                jsonMethod.setName(method.getName());
                jsonMethod.setDescription(method.getDescription());
                jsonMethod.setParameters(parameters);
                jsonMethod.setReturnValue(method.getReturnValue());

                jsonMethods.add(jsonMethod);
            }

            jsonSpxClass.setMethods(jsonMethods);

            result.add(jsonSpxClass);
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/scs-doc", method = RequestMethod.GET)
    public ModelAndView doc(ModelMap model, HttpServletRequest request) {

        String service = request.getParameter("service");

        model.addAttribute("paths", rpcResourceSupplier.getPaths());
        model.addAttribute("service", service);
        return new ModelAndView("documentation", model);
    }
}

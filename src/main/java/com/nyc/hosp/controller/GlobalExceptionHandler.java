package com.nyc.hosp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAllExceptions(Exception ex) {
        // Ignore Chrome DevTools requests
        if (ex.getMessage() != null && ex.getMessage().contains("chrome.devtools")) {
            return new ModelAndView("error/404");
        }
        
        logger.error("Unhandled exception occurred", ex);
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", ex);
        mav.addObject("message", ex.getMessage());
        mav.addObject("stackTrace", ex.getStackTrace());
        mav.setViewName("error/500");
        
        return mav;
    }
    
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception occurred", ex);
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("message", ex.getMessage());
        mav.setViewName("error/403");
        
        return mav;
    }
}

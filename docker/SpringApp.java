package org.fastercode.idgenerator;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.fastercode.idgenerator.core.generator.ID;
import org.fastercode.idgenerator.core.generator.IDGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;

@SpringBootApplication
public class SpringApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringApp.class, args);
    }

    @RestController
    @RequestMapping("/fastercode/id-generator")
    public static class Controller {
        @Autowired
        private IDGenerator idGenerator;

        @RequestMapping("/generate")
        public IDResponse generate(Long extraData) {
            ID id = extraData == null ? idGenerator.generate() : idGenerator.generate(extraData);
            IDResponse idResponse = new IDResponse();
            BeanUtils.copyProperties(id, idResponse);
            return idResponse;
        }

        @RequestMapping("/getWorkerID")
        public int getWorkerID() {
            return idGenerator.getWorkerID();
        }

        @RequestMapping("/getOnlineWorkerIDs")
        public HashMap<Object, Object> getOnlineWorkerIDs() {
            return idGenerator.getOnlineWorkerIDs();
        }
    }

    public static class IDResponse extends ID {
        @JsonFormat(pattern = "EEE MMM dd HH:mm:ss Z yyyy", locale = "en_US", timezone = "GMT+8")
        private Date createDate;
    }

}

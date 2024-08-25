package com.ashcollege;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.cloudinary.*;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;

@SpringBootApplication(exclude = HibernateJpaAutoConfiguration.class)
@EnableScheduling
public class Main {
    public static boolean applicationStarted = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(Persist.class);

    public static long startTime;

    public static void main(String[] args) {
//        // Set your Cloudinary credentials
//
//        Dotenv dotenv = Dotenv.load();
//        Cloudinary cloudinary = new Cloudinary(dotenv.get("CLOUDINARY_URL"));
//        cloudinary.config.secure = true;
//        System.out.println(cloudinary.config.cloudName);


// Upload the image
//        try {
//            Map params1 = ObjectUtils.asMap(
//                    "use_filename", true,
//                    "unique_filename", false,
//                    "overwrite", true
//            );
//
//            System.out.println(
//                    cloudinary.uploader().upload("https://cloudinary-devs.github.io/cld-docs-assets/assets/images/coffee_cup.jpg", params1));
//
//        }catch (Exception e) {
//            System.out.println("error");;
//        }

//
//
//        // Get the asset details
//        Map params2 = ObjectUtils.asMap(
//                "quality_analysis", true
//        );
//
//        System.out.println("-------------------");
//        try {
//            System.out.println(
//                    cloudinary.api().resource("coffee_cup", params2));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }



        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        LOGGER.info("Application started.");
        applicationStarted = true;
        startTime = System.currentTimeMillis();

    }


}

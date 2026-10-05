package com.systemdesign.flashsale;

import org.springframework.boot.SpringApplication;

public class TestFlashsaleApplication {

	public static void main(String[] args) {
		SpringApplication.from(FlashsaleApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

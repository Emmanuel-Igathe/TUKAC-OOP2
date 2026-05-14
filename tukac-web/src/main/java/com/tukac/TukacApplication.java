package com.tukac;

import com.tukac.model.*;
import com.tukac.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TukacApplication {
    public static void main(String[] args) {
        SpringApplication.run(TukacApplication.class, args);
    }

    @Bean
    public CommandLineRunner dataLoader(
            UserRepository userRepo,
            EventRepository eventRepo,
            BlogPostRepository blogRepo,
            TransactionRepository transRepo,
            EventRegistrationRepository regRepo) {
        return args -> {
            if (userRepo.count() > 0) return;

            org.springframework.security.crypto.password.PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

            // 1. Seed Users
            com.tukac.model.User admin = new com.tukac.model.User();
            admin.setName("System Admin"); admin.setStudentId("ADMIN001");
            admin.setEmail("admin@tukac.com"); admin.setPassword(encoder.encode("admin123"));
            admin.setRole("chairperson"); admin.setIsApproved(1);
            userRepo.save(admin);

            com.tukac.model.User exec = new com.tukac.model.User();
            exec.setName("Jane Wanjiku"); exec.setStudentId("EXE001");
            exec.setEmail("exec@tukac.ac.ke"); exec.setPassword(encoder.encode("exec123"));
            exec.setRole("vice-chairperson"); exec.setIsApproved(1);
            userRepo.save(exec);

            com.tukac.model.User member = new com.tukac.model.User();
            member.setName("John Kamau"); member.setStudentId("MEM001");
            member.setEmail("member@tukac.ac.ke"); member.setPassword(encoder.encode("member123"));
            member.setRole("member"); member.setIsApproved(1);
            userRepo.save(member);

            // 2. Seed Events
            com.tukac.model.Event ev1 = new com.tukac.model.Event();
            ev1.setTitle("Disability Awareness Week 2025");
            ev1.setDescription("Annual event to raise awareness about disability rights at TUK.");
            ev1.setEventDate("2025-06-15"); ev1.setEventTime("09:00 AM");
            ev1.setLocation("TUK Main Hall"); ev1.setCapacity(200);
            ev1.setCreatedBy(admin.getId()); eventRepo.save(ev1);

            com.tukac.model.Event ev2 = new com.tukac.model.Event();
            ev2.setTitle("Kenyan Sign Language Workshop");
            ev2.setDescription("Learn basic KSL in this interactive workshop.");
            ev2.setEventDate("2025-07-10"); ev2.setEventTime("02:00 PM");
            ev2.setLocation("Room B204"); ev2.setCapacity(50);
            ev2.setCreatedBy(exec.getId()); eventRepo.save(ev2);

            // 3. Seed Blog Posts
            com.tukac.model.BlogPost bp1 = new com.tukac.model.BlogPost();
            bp1.setTitle("Understanding Disability Rights in Kenya");
            bp1.setContent("Kenya has made significant strides in disability rights through the Constitution 2010...");
            bp1.setAuthorId(admin.getId()); bp1.setCategory("Advocacy");
            blogRepo.save(bp1);

            com.tukac.model.BlogPost bp2 = new com.tukac.model.BlogPost();
            bp2.setTitle("Sign Language: Bridging the Gap");
            bp2.setContent("Kenyan Sign Language (KSL) is the primary language of the Deaf community...");
            bp2.setAuthorId(exec.getId()); bp2.setCategory("Education");
            blogRepo.save(bp2);

            // 4. Seed Transactions
            com.tukac.model.Transaction t1 = new com.tukac.model.Transaction();
            t1.setDescription("Annual Membership Dues Collection");
            t1.setAmount(15000.0); t1.setType("income");
            t1.setCategory("Membership"); t1.setTransactionDate("2025-05-01");
            t1.setCreatedBy(admin.getId()); transRepo.save(t1);

            com.tukac.model.Transaction t2 = new com.tukac.model.Transaction();
            t2.setDescription("Event Equipment Purchase");
            t2.setAmount(8500.0); t2.setType("expense");
            t2.setCategory("Equipment"); t2.setTransactionDate("2025-05-10");
            t2.setCreatedBy(exec.getId()); transRepo.save(t2);

            System.out.println("✅ Sample data successfully seeded into all tables!");
        };
    }
}

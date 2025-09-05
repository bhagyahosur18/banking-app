# Banking App
A modular, secure, and scalable banking application built with Spring Boot and Docker. This project demonstrates clean architecture, JWT-based authentication, inter-service communication, and best practices in backend development.

### Features
| Service                 | Description                                                               |
|-------------------------|---------------------------------------------------------------------------|
| **User Service**        | Handles user registration, login, JWT issuance, and profile management    |
| **Account Service**     | Manages account creation, balance tracking, and admin-level operations    |
| **Transaction Service** | Processes deposits, withdrawals, transfers, and transaction history       |

Each service is independently deployable, Dockerized, and registered with Eureka for service discovery.

### Technologies Used

- Java 17
- Spring Boot
- Spring Security (JWT-based)
- Spring Data JPA
- PostgreSQL
- Maven
- Docker
- Eureka (Netflix OSS)
- OpenAPI (Swagger UI)

### Authentication & Security

- JWT tokens are issued by the **User Service** upon login
- Tokens must be included in the `Authorization` header for protected endpoints:
  ```http
  Authorization: Bearer <your-jwt-token>
  ```

### Docker setup
**Build and run the service**
```bash 
  docker-compose up --build 
```
- Eureka Server runs on localhost:8761
- Registers with Eureka at startup
- Services register automatically and expose ports:
    - User Service: 8081
    - Account Service: 8082
    - Transaction Service: 8083


### API Documentation

| Service                 | Description                                  |
|-------------------------|----------------------------------------------|
| **User Service**        | http://localhost:8081/swagger-ui/index.html  |
| **Account Service**     | http://localhost:8082/swagger-ui/index.html  |
| **Transaction Service** | http://localhost:8083/swagger-ui/index.html  |

### Architecture Highlights
- Clean separation of concerns: Controller → Service → Repository 
- DTOs mapped via MapStruct 
- Dockerized microservices with isolated responsibilities 
- Inter-service communication via REST (Account ↔ Transaction)
- Eureka-based service discovery 
- OpenAPI documentation for all services 
- Audit logging (optional, extensible)

### Future Improvements
- CI/CD pipeline integration 
- Centralized logging and monitoring 
- Password reset flow
- API gateway implementation

---

## Known Vulnerabilities

This project uses popular Spring Boot and Spring Cloud dependencies that include transitive libraries with known CVEs. These do not directly impact functionality but may pose risks in certain container environments or edge cases.

### 📦 spring-boot-starter-web

| Transitive Dependency                         | CVE ID                         | Issue Type                                          | Severity |
|-----------------------------------------------|--------------------------------|-----------------------------------------------------|----------|
| `org.springframework:spring-beans:6.2.9`      | CVE-2025-41242                 | Path Traversal on non-compliant containers          | 5.9      |
| `org.apache.tomcat.embed:tomcat-embed-core`   | Multiple (e.g. CVE-2021-25329) | Deserialization, Info Disclosure, Request Smuggling | 7.0–7.5  |
| `com.fasterxml.jackson.core:jackson-databind` | CVE-2020-36518                 | Out-of-bounds Write                                 | 7.5      |

### 📦 spring-cloud-starter-netflix-eureka-server

| Transitive Dependency                             | CVE ID                   | Issue Type                        | Severity |
|---------------------------------------------------|--------------------------|-----------------------------------|----------|
| `com.thoughtworks.xstream:xstream:1.4.20`         | CVE-2024-47072           | Deserialization of Untrusted Data | 7.5      |

> ⚠️ This project is for educational and demonstration purposes. While known CVEs are monitored and mitigated, production deployments should include dependency scanning, container hardening, and regular patching.

---


### License

This project is licensed under the MIT License.

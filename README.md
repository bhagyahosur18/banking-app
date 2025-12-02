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

## CI/CD Pipeline

### Overview
This project uses **GitHub Actions** for continuous integration and deployment. Every push triggers automated testing, building, and Docker image publishing.

### Workflow
The CI/CD pipeline is defined in `.github/workflows/ci-cd.yml` and includes:

#### 1. **Build & Test Job** (`build-and-test`)
Runs on every push to `main` and `develop` branches:
- Checks out code from GitHub
- Sets up Java 17 with Maven
- Builds all three Spring Boot services
- Runs unit tests
- Runs integration tests with PostgreSQL database service
- Uploads test results as artifacts

**Trigger:** Every push to `main` or `develop`, or pull request

#### 2. **Docker Build & Push Job** (`build-docker-images`)
Runs ONLY after tests pass AND only on `main` branch:
- Sets up Docker Buildx for multi-architecture builds
- Authenticates with Docker Hub
- Builds Docker images for all services:
    - `user-service`
    - `account-service`
    - `transaction-service`
    - `eureka-server`
- Builds for both `linux/amd64` and `linux/arm64` architectures
- Pushes images to Docker Hub with tags: `latest` and git commit SHA

**Trigger:** Only on successful `main` branch builds

### Services Built
| Service             | Image                                      | Port |
|---------------------|--------------------------------------------|------|
| User Service        | `your-username/user-service:latest`        | 8081 |
| Account Service     | `your-username/account-service:latest`     | 8082 |
| Transaction Service | `your-username/transaction-service:latest` | 8083 |
| Eureka Server       | `your-username/eureka-server:latest`       | 8761 |

### Local Testing
To run services locally with Docker:
```bash
# Start PostgreSQL
docker run -d --name postgres-db --network banking-network \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:15

# Create databases
docker exec -it postgres-db psql -U postgres -c "CREATE DATABASE usersdb;"
docker exec -it postgres-db psql -U postgres -c "CREATE DATABASE accountdb;"
docker exec -it postgres-db psql -U postgres -c "CREATE DATABASE transactiondb;"

# Start services
docker run -p 8081:8081 --network banking-network \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-db:5432/usersdb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e JWT_SECRET=your-secret-key \
  your-username/user-service:latest

docker run -p 8082:8082 --network banking-network \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-db:5432/accountdb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e JWT_SECRET=your-secret-key \
  your-username/account-service:latest

docker run -p 8083:8083 --network banking-network \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-db:5432/transactiondb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e JWT_SECRET=your-secret-key \
  your-username/transaction-service:latest
```

### Environment Variables
| Variable                     | Description             | Example                                      |
|------------------------------|-------------------------|----------------------------------------------|
| `SPRING_DATASOURCE_URL`      | Database connection URL | `jdbc:postgresql://postgres-db:5432/usersdb` |
| `SPRING_DATASOURCE_USERNAME` | Database username       | `postgres`                                   |
| `SPRING_DATASOURCE_PASSWORD` | Database password       | `postgres`                                   |
| `JWT_SECRET`                 | JWT token signing key   | `your-secret-key`                            |

### GitHub Secrets Required
Configure these in your GitHub repository settings (Settings → Secrets and variables → Actions):
- `DOCKER_USERNAME` - Your Docker Hub username
- `DOCKER_PASSWORD` - Your Docker Hub password or access token


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
- Centralized logging and monitoring 
- Password reset flow
- API gateway implementation


### License

This project is licensed under the MIT License.

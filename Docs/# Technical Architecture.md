# Technical Architecture

**Architectural Style:** Distributed System using Microservices and an Event-Driven Architecture (EDA).

## System Context Diagram (C4 Level 1)

```mermaid
graph TD
    subgraph "Agri-Enhancement Platform (AEP)"
        AEP_System[("<b>Agri-Enhancement Platform</b><br/>CMS/ERP for agricultural enhancement")]
    end

    subgraph "Users"
        Farmer[<i class='fa fa-user'></i> Smallholder Farmer]
        Agent[<i class='fa fa-user'></i> Field Agent]
        CoopManager[<i class='fa fa-user'></i> Cooperative Manager]
        Agribusiness[<i class='fa fa-user'></i> Agribusiness User]
        Admin[<i class='fa fa-user'></i> Platform Administrator]
    end

    subgraph "External Systems"
        PaymentGateway[<i class='fa fa-university'></i> Payment Gateway]
        SMSGateway[<i class='fa fa-comments'></i> SMS/USSD Gateway]
        WeatherAPI[<i class='fa fa-cloud-sun'></i> Weather Service API]
        SatelliteAPI[<i class='fa fa-satellite'></i> Satellite Imagery API]
        GISService[<i class='fa fa-map'></i> GIS/Mapping Service]
    end

    Farmer -- "Uses Mobile App / USSD/SMS" --> AEP_System
    Agent -- "Uses Mobile App" --> AEP_System
    CoopManager -- "Uses Web & Mobile Apps" --> AEP_System
    Agribusiness -- "Uses Web Portal" --> AEP_System
    Admin -- "Uses Web Portal" --> AEP_System

    AEP_System -- "Initiates Payments / Receives Confirmations" --> PaymentGateway
    AEP_System -- "Sends/Receives SMS & USSD" --> SMSGateway
    AEP_System -- "Requests Weather Data" --> WeatherAPI
    AEP_System -- "Fetches Satellite Imagery" --> SatelliteAPI
    AEP_System -- "Fetches Basemaps / Geocoding" --> GISService
```

## Container Diagram (C4 Level 2)

```mermaid
graph TD
    subgraph "External Systems"
        direction LR
        Ext_SMS[SMS/USSD Gateway]
        Ext_Payment[Payment Gateway]
        Ext_Satellite[Satellite Imagery API]
    end

    subgraph "Users"
        direction LR
        User_Mobile[Mobile App User]
        User_Web[Web Portal User]
        User_USSD[USSD/SMS User]
    end

    subgraph "Agri-Enhancement Platform (AEP)"
        direction TB
        
        subgraph "Client-Facing Layer"
            MobileApp[("<b>Mobile App</b><br/>[React Native / Flutter]<br/>Offline-first PWA")]
            WebApp[("<b>Web Application</b><br/>[Angular / React]<br/>Admin & Analytics Portal")]
            APIGateway[("<b>API Gateway</b><br/>[Spring Cloud Gateway]<br/>Auth, Routing, Rate Limiting")]
            USSDHandler[("<b>USSD/SMS Handler</b><br/>[Node.js / Java]<br/>Processes basic phone interactions")]
        end

        subgraph "Backend Microservices"
            IAM_Service[("<b>IAM Service</b><br/>[Java/Spring Boot]<br/>Users, Tenants, Roles")]
            FarmPlot_Service[("<b>Farm & Plot Service</b><br/>[Java/Spring Boot]<br/>Manages farms, plots, geospatial data")]
            AgPractices_Service[("<b>Ag Practices Service</b><br/>[Java/Spring Boot]<br/>Crops, Activities, Yields")]
            IoT_Service[("<b>IoT Ingestion Service</b><br/>[Akka / Vert.x]<br/>Receives sensor data")]
            Imagery_Service[("<b>Imagery Analysis Service</b><br/>[Python/FastAPI]<br/>Processes satellite images")]
            Finance_Service[("<b>Finance Service</b><br/>[Java/Spring Boot]<br/>Integrates with payment gateways")]
        end

        subgraph "Data Infrastructure"
            EventBus[("<b>Event Bus</b><br/>[Apache Kafka / Pulsar]<br/>Asynchronous communication")]
            RelationalDB[("<b>Relational DB</b><br/>[PostgreSQL + PostGIS]<br/>Transactional Data")]
            TimeSeriesDB[("<b>Time-Series DB</b><br/>[TimescaleDB / InfluxDB]<br/>Sensor Data")]
            ObjectStorage[("<b>Object Storage</b><br/>[AWS S3 / MinIO]<br/>Images, Documents")]
        end

        User_Mobile --> MobileApp; User_Web --> WebApp; User_USSD --> USSDHandler;
        MobileApp --> APIGateway; WebApp --> APIGateway; USSDHandler --> APIGateway;
        USSDHandler -- "Sends/Receives" --> Ext_SMS;
        APIGateway --> IAM_Service; APIGateway --> FarmPlot_Service; APIGateway --> AgPractices_Service; APIGateway --> Finance_Service;
        FarmPlot_Service -- "CRUD" --> RelationalDB; IAM_Service -- "CRUD" --> RelationalDB; AgPractices_Service -- "CRUD" --> RelationalDB; Finance_Service -- "CRUD" --> RelationalDB; Imagery_Service -- "CRUD" --> ObjectStorage;
        IoT_Service -- "Ingests" --> EventBus; EventBus --> TimeSeriesDB;
        FarmPlot_Service -- "Pub/Sub Events" --> EventBus; AgPractices_Service -- "Pub/Sub Events" --> EventBus;
        Finance_Service -- "API Calls" --> Ext_Payment; Imagery_Service -- "API Calls" --> Ext_Satellite;
    end
```


# MercadoX Email & Notification Service

## Overview

`mercado-x-email` is an event-driven microservice responsible for handling notifications.

It consumes Kafka events and triggers external communication providers.

---

## Responsibilities

- Kafka event listeners
- Email delivery (Spring Mail)
- WhatsApp Business API integration
- Notification template processing
- Multi-tenant message handling

---

## Architecture

- Kafka consumer
- External API client (WebClient)
- Template-based messaging
- orgId-aware processing

---

## Triggered By

- mercado-x-core events
- Domain event publications

---

## External Integrations

- SMTP (Spring Mail)
- WhatsApp Business API
# Final V1 Architecture

- `app/src/main/java/.../data/local`: Room entities, DAOs, database.
- `data/repo`: repository wrapper over DAOs.
- `storage`: local file import/copy/delete in app-private storage.
- `backup`: JSON backup models and SAF export manager.
- `ui/screens`: Compose screens + navigation state.

## Data model
- PatientEntity
- MedicalDocumentEntity
- VisitEntity
- VisitDocumentCrossRef

## Principles
- Offline local-first.
- No backend/cloud.
- Simple UX for quick document retrieval during consultations.

# Authorization-Common Mandates

This module provides the aspect-oriented authorization framework for Cloudbreak APIs.

## 🛡 Authorization Rules
- **API Level**: Annotations (`@CheckPermissionByResourceCrn`, etc.) belong on **Controller** classes.
- **Validation Level**: Validation annotations belong on **Endpoint** classes.
- **Internal APIs**: Use `@InternalOnly` to bypass UMS, but ensure the API remains "internally callable" by providing `@ResourceCrn`, `@AccountId`, or `@InitiatorUserCrn`.
- **Default Resources**: Use `DefaultResourceChecker` for system-wide defaults (like image catalogs) that bypass UMS.

## 📂 Design Mandates
- **Compliance**: Every module importing this framework must include an `EnforceAuthorizationAnnotationsTest` to verify that all controllers have mandatory authorization annotations.
- **List Filtering**: Extend `AbstractAuthorizationFiltering` for resource listing; avoid loading full entities from the DB before filtering.

## 🔍 Key Symbols
- **Actions**: `AuthorizationResourceAction`.
- **Types**: `AuthorizationResourceType`.
- **Logic**: `AuthorizationFactory`, `AuthorizationRule`.

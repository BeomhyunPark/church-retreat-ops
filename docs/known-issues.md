# Known Issues

## Community API controller disabled

Status: blocks formal production deployment; does not block the limited demo deployment.

`AdminCommunityController` is currently commented out, so requests under
`/api/admin/community` return HTTP 404. The shared `createMiddleGroup` test helper expects HTTP
200, which causes seven integration tests to fail:

- `createAnnouncementSupportsRetreatGroupAndChurchTargets`
- `duplicateCommunityNamesFollowMiddleGroupAndCellRules`
- `staffCanReadCommunityDataButCannotCreateOrUpdateIt`
- `communityTreeIncludesMiddleGroupsAndCells`
- `chairCanCreateAndPastorCanUpdateCommunityStructure`
- `checkInRosterSupportsFilters`
- `chairCanLinkAndUnlinkParticipantChurchCellWithoutChangingFreeTextDepartment`

Reproduction:

```bash
./gradlew clean test
```

Resolution criteria:

- restore or intentionally replace the community API controller;
- preserve the existing role hierarchy and participant/admin API separation;
- make all seven affected tests pass;
- run the complete `./gradlew clean test` suite successfully before formal production approval.

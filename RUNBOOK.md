# Manual SIT smoke test

These calls hit real downstream systems (the Bakong OB hub, the CIFTP SOAP adapter) and require a `.env`
populated with valid SIT credentials/URLs (see `README.md`) — they cannot run in CI/this environment.

## Account Inquiry to Other Bank

```shell
curl -s "http://localhost:8080/api/v1/account-inquiry-to-other-bank?accountNumber=000123456789&participantCode=ABCD" | jq .
```

Expected happy-path shape:

```json
{
  "status": { "code": 0, "errorCode": null, "error": null, "warning": null, "message": null },
  "data": {
    "result": "success",
    "product_code": "BAKONG",
    "account_data": {
      "account": "000123456789",
      "kycStatus": "1",
      "account_name": "...",
      "account_currency": "USD",
      "bank_code": "...",
      "account_type": "01"
    },
    "publicOperations": ["RETAIL", "LARGE_VALUE"]
  }
}
```

An account that doesn't exist at the other bank should come back as:

```json
{ "status": { "code": 1, "errorCode": 65, "error": "The Account Local Number is not found.", "warning": "", "message": "" }, "data": null }
```

## Full Fund Transfer

```shell
curl -s -X POST "http://localhost:8080/api/v1/bakong-full-fund-transfer" \
  -H "Content-Type: application/json" \
  -d '{
    "createDateTime": "2026-09-15T10:00:00",
    "requestExactDate": "2026-09-15",
    "requestMsgId": "MSG-0001",
    "txnAmount": 10.00,
    "receiverBankCode": "OTHRKHPPXXX",
    "debitAccountNo": "000111222333",
    "txnCurrency": "USD",
    "creditAccountName": "CREDITOR NAME",
    "paymentEndToEndId": "e2e-0001",
    "creditAccountNo": "000999888777",
    "debitAccountName": "DEBTOR NAME",
    "extRefId": "EXTREF-0001",
    "paymentInstrId": "instr-0001",
    "paymentInfoId": "PMTINFO-0001",
    "txnDescription": "SIT smoke test"
  }' | jq .
```

Expected happy-path shape:

```json
{
  "status": { "code": 0, "errorCode": null, "errorMessage": null },
  "data": { "requestMsgId": "MSG-0001", "paymentInfoId": "PMTINFO-0001", "hash": "..." }
}
```

## Sign-off checklist before promoting past SIT

- [ ] Account inquiry happy path returns correct field mapping for a known SIT test account.
- [ ] Account inquiry "not found" (errorCode 015/15) maps to `errorCode: 65`.
- [ ] Full fund transfer happy path completes and the CIFTP-side ledger shows the debit/credit.
- [ ] Full fund transfer against a duplicate `extRefId`/`requestMsgId` returns a clean SOAP-fault error
      envelope, not a raw 500.
- [ ] Confirm with the Bakong OB hub contract owner whether `accountInfo.localPartCode` is really populated
      in production responses (see README "Known legacy quirks").

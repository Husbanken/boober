package deploy

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  request {
    method 'POST'
    url $(
        consumer(~/\/v1\/vault\/[a-z]+\//),
        producer('/v1/vault/vaultcollection/')
    )
    headers {
      contentType(applicationJson())
    }
    body(
        operationName: 'reencrypt',
        parameters: [
          'encryptionKey': 'key'
        ]
    )
  }
  response {
    status 200
  }
}
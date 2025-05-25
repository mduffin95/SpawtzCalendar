package com.mduffin95.spawtzcalendar.persistence

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream

suspend fun putObject(
    bucketName: String,
    objectKey: String,
    calendarString: ByteStream,
    contentTypeString: String? = null
) {
    val request =
        PutObjectRequest.Companion {
            bucket = bucketName
            key = objectKey
            body = calendarString
            contentType = contentTypeString
        }

    S3Client.Companion { region = "eu-west-1" }.use { s3 ->
        val response = s3.putObject(request)
        println("Tag information is ${response.eTag}")
    }
}
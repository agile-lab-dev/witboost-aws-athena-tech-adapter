# AWS permissions

Below is an example of an IAM policy including the permissions to be assigned to the role assumed by the tech adapter in order for it to function properly.

For simplicity, resources are indicated with `"*"`. Restrict the list according to your needs

```json
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Sid": "Glue",
			"Effect": "Allow",
			"Action": [
				"glue:GetDatabases",
				"glue:GetDatabase",
				"glue:getTables",
				"glue:CreateDatabase",
				"glue:CreateTable",
				"glue:GetCatalogs",
				"glue:GetCatalog",
				"glue:UpdateTable",
				"glue:DeleteTable"
			],
			"Resource": "*"
		},
		{
			"Sid": "GlueGetTable",
			"Effect": "Allow",
			"Action": [
				"glue:GetTable"
			],
			"Resource": [
				"arn:aws:glue:{region}:{accountID}:catalog",
				"arn:aws:glue:{region}:{accountID}:database/*",
				"arn:aws:glue:{region}:{accountID}:table/*"
			]
		},
		{
			"Sid": "Athena",
			"Effect": "Allow",
			"Action": [
				"athena:StartQueryExecution",
				"athena:GetQueryResults",
				"athena:GetQueryExecution",
				"athena:GetWorkGroup",
				"athena:ListQueryExecutions",
				"athena:BatchGetQueryExecution",
				"athena:ListDatabases",
				"athena:ListDataCatalogs",
				"athena:ListTableMetadata"
			],
			"Resource": [
				"*"
			]
		},
		{
			"Sid": "S3",
			"Effect": "Allow",
			"Action": [
				"s3:GetBucketLocation",
				"s3:GetObject",
				"s3:PutObject"
			],
			"Resource": [
				"*"
			]
		},
		{
			"Sid": "LakeFormationAndIam",
			"Effect": "Allow",
			"Action": [
				"lakeformation:GetDataAccess",
				"lakeformation:RegisterResource",
				"lakeformation:GrantPermissions",
				"iam:PassRole",
				"iam:GetRole"
			],
			"Resource": [
				"*"
			]
		},
		{
			"Sid": "Kms",
			"Effect": "Allow",
			"Action": [
				"kms:Decrypt",
				"kms:GenerateDataKey",
				"kms:Encrypt"
			],
			"Resource": [
				"*"
			]
		}
	]
}
```

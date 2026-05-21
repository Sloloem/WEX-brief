local:
	gradlew bootBuildImage
docker:
	docker-compose up --force-recreate --detach TransactionDB
test:
	gradlew check
compose: test local docker
quick: local docker
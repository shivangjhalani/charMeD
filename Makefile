.PHONY: build build-backend build-frontend run test clean

build: build-backend build-frontend

build-backend:
	cd backend && ./gradlew shadowJar

build-frontend:
	cd frontend && go build -o charmed .

run: build
	./frontend/charmed

test:
	cd backend && ./gradlew test

clean:
	cd backend && ./gradlew clean
	rm -f frontend/charmed

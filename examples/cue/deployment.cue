apiVersion: "apps/v1"
kind:       "Deployment"
metadata: name: "hello-kubernetes"
spec: {
	replicas: 3
	selector: matchLabels: app: "hello-kubernetes"
	template: {
		metadata: labels: app: "hello-kubernetes"
		spec: containers: [{
			name:  "hello-kubernetes"
			image: "paulbouwer/hello-kubernetes:1.5"
			ports: [{
				containerPort: 8081
			}]
		}]
	}
}
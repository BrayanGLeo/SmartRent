export const environment = {
  production: true,
  azure: {
    clientId: 'f5890cb5-fba6-429e-b886-323d05ea886c',
    tenantId: '753ae9df-d5ec-4c6b-8227-b334fc775087',
    authority: 'https://753ae9df-d5ec-4c6b-8227-b334fc775087.ciamlogin.com/753ae9df-d5ec-4c6b-8227-b334fc775087/v2.0',
    redirectUri: 'http://<AWS_EC2_PUBLIC_IP>/', // UPDATE WITH ACTUAL EC2 IP
    apiScope: 'api://749dc676-d557-460a-b138-dac9a6744b6e/Arriendos.Leer'
  },
  apiBaseUrl: 'http://<AWS_EC2_PUBLIC_IP>:8080/api' // UPDATE WITH ACTUAL EC2 IP
};
